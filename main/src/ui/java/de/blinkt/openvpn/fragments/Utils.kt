/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.fragments

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.provider.OpenableColumns
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.webkit.MimeTypeMap
import de.blinkt.openvpn.R
import kotlin.Throws
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.Preferences
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

object Utils {
    @JvmStatic
    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun getFilePickerIntent(c: Context, fileType: FileType?): Intent? {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        val supportedMimeTypes = TreeSet<String>()
        val extensions = Vector<String>()
        when (fileType) {
            FileType.PKCS12 -> {
                i.type = "application/x-pkcs12"
                supportedMimeTypes.add("application/x-pkcs12")
                extensions.add("p12")
                extensions.add("pfx")
            }
            FileType.CLIENT_CERTIFICATE, FileType.CA_CERTIFICATE -> {
                i.type = "application/x-pem-file"
                supportedMimeTypes.add("application/x-x509-ca-cert")
                supportedMimeTypes.add("application/x-x509-user-cert")
                supportedMimeTypes.add("application/x-pem-file")
                supportedMimeTypes.add("application/pkix-cert")
                supportedMimeTypes.add("text/plain")
                extensions.add("pem")
                extensions.add("crt")
                extensions.add("cer")
            }
            FileType.KEYFILE -> {
                i.type = "application/x-pem-file"
                supportedMimeTypes.add("application/x-pem-file")
                supportedMimeTypes.add("application/pkcs8")

                // Google drive ....
                supportedMimeTypes.add("application/x-iwork-keynote-sffkey")
                extensions.add("key")
            }
            FileType.TLS_AUTH_FILE -> {
                i.type = "text/plain"

                // Backup ....
                supportedMimeTypes.add("application/pkcs8")
                // Google Drive is kind of crazy .....
                supportedMimeTypes.add("application/x-iwork-keynote-sffkey")
                extensions.add("txt")
                extensions.add("key")
            }
            FileType.OVPN_CONFIG -> {
                i.type = "application/x-openvpn-profile"
                supportedMimeTypes.add("application/x-openvpn-profile")
                supportedMimeTypes.add("application/openvpn-profile")
                supportedMimeTypes.add("application/ovpn")
                supportedMimeTypes.add("text/plain")
                extensions.add("ovpn")
                extensions.add("conf")
            }
            FileType.CRL_FILE -> {
                supportedMimeTypes.add("application/x-pkcs7-crl")
                supportedMimeTypes.add("application/pkix-crl")
                extensions.add("crl")
            }
            FileType.USERPW_FILE -> {
                i.type = "text/plain"
                supportedMimeTypes.add("text/plain")
            }
        }
        val mtm = MimeTypeMap.getSingleton()
        for (ext in extensions) {
            val mimeType = mtm.getMimeTypeFromExtension(ext)
            if (mimeType != null) supportedMimeTypes.add(mimeType)
        }

        // Always add this as fallback
        supportedMimeTypes.add("application/octet-stream")
        i.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes.toTypedArray())

        // People don't know that this is actually a system setting. Override it ...
        // DocumentsContract.EXTRA_SHOW_ADVANCED is hidden
        i.putExtra("android.content.extra.SHOW_ADVANCED", true)

        /* Samsung has decided to do something strange, on stock Android GET_CONTENT opens the document UI */
        /* fist try with documentsui */if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) i.setPackage(
            "com.android.documentsui"
        )


        /*
         * Android 11 is much stricter about allowing what to query. Since the app has the
         * QUERY_ALL permission we can still check on Android 11 but otherwise we would just
         * assume the documents ui to be always there:
         */

        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return i;
        }*/
        if (!isIntentAvailable(c, i)) {
            i.action = Intent.ACTION_OPEN_DOCUMENT
            i.setPackage(null)

            // Check for really broken devices ... :(
            if (!isIntentAvailable(c, i)) {
                return null
            }
        }


        /*
        final PackageManager packageManager = c.getPackageManager();
        ResolveInfo list = packageManager.resolveActivity(i, 0);

        Toast.makeText(c, "Starting package: "+ list.activityInfo.packageName
                + "with ACTION " + i.getAction(), Toast.LENGTH_LONG).show();

        */return i
    }

    @JvmStatic
    fun alwaysUseOldFileChooser(c: Context?): Boolean {
        /* Android P does not allow access to the file storage anymore */
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return false
        val prefs = Preferences.getDefaultSharedPreferences(c)
        return prefs.getBoolean("useInternalFileSelector", false)
    }

    fun isIntentAvailable(context: Context, i: Intent?): Boolean {
        val packageManager = context.packageManager
        val list = packageManager.queryIntentActivities(
            i!!,
            PackageManager.MATCH_DEFAULT_ONLY
        )

        // Ignore the Android TV framework app in the list
        var size = list.size
        for (ri in list) {
            // Ignore stub apps
            if ("com.google.android.tv.frameworkpackagestubs" == ri.activityInfo.packageName) {
                size--
            }
        }
        return size > 0
    }

    @Throws(IOException::class)
    private fun readBytesFromStream(input: InputStream?): ByteArray {
        val buffer = ByteArrayOutputStream()
        var nRead: Int
        val data = ByteArray(16384)
        var totalread: Long = 0
        while (input!!.read(data, 0, data.size)
                .also { nRead = it } != -1 && totalread < VpnProfile.MAX_EMBED_FILE_SIZE
        ) {
            buffer.write(data, 0, nRead)
            totalread += nRead.toLong()
        }
        buffer.flush()
        input.close()
        return buffer.toByteArray()
    }

    @JvmStatic
    @Throws(IOException::class, SecurityException::class)
    fun getFilePickerResult(ft: FileType?, result: Intent?, c: Context): String? {
        val uri = result?.data ?: return null
        val fileData = readBytesFromStream(c.contentResolver.openInputStream(uri))
        var newData: String? = null
        val cursor = c.contentResolver.query(uri, null, null, null, null)
        var prefix = ""
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val cidx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cidx != -1) {
                    val displayName = cursor.getString(cidx)
                    if (!displayName.contains(VpnProfile.INLINE_TAG) && !displayName.contains(
                            VpnProfile.DISPLAYNAME_TAG
                        )
                    ) prefix = VpnProfile.DISPLAYNAME_TAG + displayName
                }
            }
        } finally {
            cursor?.close()
        }
        newData = when (ft) {
            FileType.PKCS12 -> Base64.encodeToString(
                fileData,
                Base64.DEFAULT
            )
            else -> String(fileData, Charset.forName("UTF-8"))
        }
        return prefix + VpnProfile.INLINE_TAG + newData
    }

    enum class FileType(val value: Int) {
        PKCS12(0), CLIENT_CERTIFICATE(1), CA_CERTIFICATE(2), OVPN_CONFIG(3), KEYFILE(4), TLS_AUTH_FILE(
            5
        ),
        USERPW_FILE(6), CRL_FILE(7);

        companion object {
            fun getFileTypeByValue(value: Int): FileType? {
                return when (value) {
                    0 -> PKCS12
                    1 -> CLIENT_CERTIFICATE
                    2 -> CA_CERTIFICATE
                    3 -> OVPN_CONFIG
                    4 -> KEYFILE
                    5 -> TLS_AUTH_FILE
                    6 -> USERPW_FILE
                    7 -> CRL_FILE
                    else -> null
                }
            }
        }
    }

    /* These functions make assumptions about the R.arrays.compat_mode contents */
    @JvmStatic
    fun mapCompatVer(ver: Int): Int {
        if (ver == 0 || ver >= 20600)
            return 0
        else if (ver < 20400)
            return 3
        else if (ver < 20500)
            return 2
        else if (ver < 20600)
            return 1
        return 0
    }

    @JvmStatic

    fun mapCompatMode(mode: Int): Int {
        when (mode) {
            0 -> return 0
            1 -> return 20500
            2 -> return 20400
            3 -> return 20300
            else -> return 0
        }
    }

    val seclevleregex = Regex("tls-cipher.*@SECLEVEL=0")

    @JvmStatic
    fun getWarningText(c:Context, vp:VpnProfile): SpannableStringBuilder {
        val warnings = mutableListOf<String>()

        val errorId = vp.checkProfile(c)
        if (errorId != R.string.no_error_found)
        {
            warnings.add(c.resources.getString(errorId))
        }

        addSoftWarnings(warnings, vp)
        val builder = SpannableStringBuilder()
        if (warnings.size > 0) {
            val warnSpan = SpannableString( warnings.joinToString(separator = ", "))
            warnSpan.setSpan(ForegroundColorSpan(Color.RED), 0, warnSpan.length, 0)
            builder.append(warnSpan)
        }

        return builder
    }

    val weakCiphers = listOf("BF-CBC", "DES-CBC", "NONE")

    @JvmStatic
    fun addSoftWarnings(warnings:MutableList<String>, vp:VpnProfile) {
        if (vp.mUseLegacyProvider)
            warnings.add("legacy Provider enabled")
        if (vp.mAuthenticationType == VpnProfile.TYPE_STATICKEYS)
            warnings.add("deprecated static key (--secret) mode")
        if (vp.mUseCustomConfig && vp.mCustomConfigOptions.contains(seclevleregex))
            warnings.add("low security (@SECLEVEL=0)")
        if (vp.mCompatMode > 0 )
            warnings.add("compat mode enabled")

        if ("insecure".equals(vp.mTlSCertProfile))
            warnings.add("low security (TLS security profile 'insecure' selected)");

        var cipher= vp.mCipher?.toUpperCase(Locale.ROOT)
        if (cipher.isNullOrEmpty())
            cipher = "BF-CBC";

        for (weakCipher in weakCiphers) {
            if ((vp.mDataCiphers != null && vp.mDataCiphers.toUpperCase(Locale.ROOT)
                    .contains(weakCipher))
                || (vp.mCompatMode in 1..20399 && (cipher == weakCipher))
            )
                warnings.add("weak cipher (${weakCipher})")
        }
    }

}