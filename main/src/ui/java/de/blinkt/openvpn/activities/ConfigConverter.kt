/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.security.KeyChain
import android.text.TextUtils
import android.util.Base64
import android.util.Pair
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConfigParser
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.fragments.Utils
import de.blinkt.openvpn.views.FileSelectLayout
import de.blinkt.openvpn.views.FileSelectLayout.FileSelectCallback
import java.io.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*

class ConfigConverter : BaseActivity(), FileSelectCallback, View.OnClickListener {

    private var mResult: VpnProfile? = null

    @Transient
    private var mPathsegments: List<String>? = null

    private var mAliasName: String? = null


    private val fileSelectMap = HashMap<Utils.FileType, FileSelectLayout?>()
    private var mEmbeddedPwFile: String? = null
    private val mLogEntries = Vector<String>()
    private var mSourceUri: Uri? = null
    private lateinit var mProfilename: EditText
    private var mImportTask: AsyncTask<Void, Void, Int>? = null
    private lateinit var mLogLayout: LinearLayout
    private lateinit var mProfilenameLabel: TextView

    override fun onClick(v: View) {
        if (v.id == R.id.fab_save)
            userActionSaveProfile()
        if (v.id == R.id.permssion_hint && Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
            doRequestSDCardPermission(PERMISSION_REQUEST_EMBED_FILES)

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun doRequestSDCardPermission(requestCode: Int) {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Permission declined, do nothing
        if (grantResults.size == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
            return

        // Reset file select dialogs
        findViewById<View>(R.id.files_missing_hint).visibility = View.GONE
        findViewById<View>(R.id.permssion_hint).visibility = View.GONE
        val fileroot = findViewById<View>(R.id.config_convert_root) as LinearLayout
        var i = 0
        while (i < fileroot.childCount) {
            if (fileroot.getChildAt(i) is FileSelectLayout)
                fileroot.removeViewAt(i)
            else
                i++
        }

        if (requestCode == PERMISSION_REQUEST_EMBED_FILES)
            embedFiles(null)
        else if (requestCode == PERMISSION_REQUEST_READ_URL) {
            if (mSourceUri != null)
                doImportUri(mSourceUri!!)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.cancel) {
            setResult(Activity.RESULT_CANCELED)
            finish()
        } else if (item.itemId == R.id.ok) {
            return userActionSaveProfile()
        }

        return super.onOptionsItemSelected(item)

    }

    private fun userActionSaveProfile(): Boolean {
        if (mResult == null) {
            log(R.string.import_config_error)
            Toast.makeText(this, R.string.import_config_error, Toast.LENGTH_LONG).show()
            return true
        }

        mResult!!.mName = mProfilename.text.toString()
        val vpl = ProfileManager.getInstance(this)
        if (vpl.getProfileByName(mResult!!.mName) != null) {
            mProfilename.error = getString(R.string.duplicate_profile_name)
            return true
        }

        val `in` = installPKCS12()

        if (`in` != null)
            startActivityForResult(`in`, RESULT_INSTALLPKCS12)
        else
            saveProfile()

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (mResult != null)
            outState.putSerializable(VPNPROFILE, mResult)
        outState.putString("mAliasName", mAliasName)


        val logentries = mLogEntries.toTypedArray()

        outState.putStringArray("logentries", logentries)

        val fileselects = IntArray(fileSelectMap.size)
        var k = 0
        for (key in fileSelectMap.keys) {
            fileselects[k] = key.value
            k++
        }
        outState.putIntArray("fileselects", fileselects)
        outState.putString("pwfile", mEmbeddedPwFile)
        outState.putParcelable("mSourceUri", mSourceUri)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, result: Intent?) {
        if (requestCode == RESULT_INSTALLPKCS12 && resultCode == Activity.RESULT_OK) {
            showCertDialog()
        }

        if (resultCode == Activity.RESULT_OK && requestCode >= CHOOSE_FILE_OFFSET) {
            val type = Utils.FileType.getFileTypeByValue(requestCode - CHOOSE_FILE_OFFSET)


            val fs = fileSelectMap[type]
            fs!!.parseResponse(result, this)

            val data = fs.data

            when (type) {
                Utils.FileType.USERPW_FILE -> mEmbeddedPwFile = data
                Utils.FileType.PKCS12 -> mResult!!.mPKCS12Filename = data
                Utils.FileType.TLS_AUTH_FILE -> mResult!!.mTLSAuthFilename = data
                Utils.FileType.CA_CERTIFICATE -> mResult!!.mCaFilename = data
                Utils.FileType.CLIENT_CERTIFICATE -> mResult!!.mClientCertFilename = data
                Utils.FileType.KEYFILE -> mResult!!.mClientKeyFilename = data
                Utils.FileType.CRL_FILE -> mResult!!.mCrlFilename = data
                else -> throw RuntimeException("Type is wrong somehow?")
            }
        }

        super.onActivityResult(requestCode, resultCode, result)
    }

    private fun saveProfile() {
        val result = Intent()
        val vpl = ProfileManager.getInstance(this)

        if (!TextUtils.isEmpty(mEmbeddedPwFile))
            ConfigParser.useEmbbedUserAuth(mResult, mEmbeddedPwFile)

        vpl.addProfile(mResult)
        vpl.saveProfile(this, mResult)
        vpl.saveProfileList(this)
        result.putExtra(VpnProfile.EXTRA_PROFILEUUID, mResult!!.uuid.toString())
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    fun showCertDialog() {
        try {

            KeyChain.choosePrivateKeyAlias(this,
                    { alias ->
                        // Credential alias selected.  Remember the alias selection for future use.
                        mResult!!.mAlias = alias
                        saveProfile()
                    },
                    arrayOf("RSA", "EC"), null, // issuer, null for any
                    mResult!!.mServerName, // host name of server requesting the cert, null if unavailable
                    -1, // port of server requesting the cert, -1 if unavailable
                    mAliasName)// List of acceptable key types. null for any
            // alias to preselect, null if unavailable
        } catch (anf: ActivityNotFoundException) {
            val ab = AlertDialog.Builder(this)
            ab.setTitle(R.string.broken_image_cert_title)
            ab.setMessage(R.string.broken_image_cert)
            ab.setPositiveButton(android.R.string.ok, null)
            ab.show()
        }

    }


    private fun installPKCS12(): Intent? {

        if (!(findViewById<View>(R.id.importpkcs12) as CheckBox).isChecked) {
            setAuthTypeToEmbeddedPKCS12()
            return null

        }
        var pkcs12datastr = mResult!!.mPKCS12Filename
        if (VpnProfile.isEmbedded(pkcs12datastr)) {
            val inkeyIntent = KeyChain.createInstallIntent()

            pkcs12datastr = VpnProfile.getEmbeddedContent(pkcs12datastr)


            val pkcs12data = Base64.decode(pkcs12datastr, Base64.DEFAULT)


            inkeyIntent.putExtra(KeyChain.EXTRA_PKCS12, pkcs12data)

            if (mAliasName == "")
                mAliasName = null

            if (mAliasName != null) {
                inkeyIntent.putExtra(KeyChain.EXTRA_NAME, mAliasName)
            }
            return inkeyIntent

        }
        return null
    }


    private fun setAuthTypeToEmbeddedPKCS12() {
        if (VpnProfile.isEmbedded(mResult!!.mPKCS12Filename)) {
            if (mResult!!.mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE)
                mResult!!.mAuthenticationType = VpnProfile.TYPE_USERPASS_PKCS12

            if (mResult!!.mAuthenticationType == VpnProfile.TYPE_KEYSTORE)
                mResult!!.mAuthenticationType = VpnProfile.TYPE_PKCS12

        }
    }


    private fun getUniqueProfileName(possibleName: String?): String {

        var i = 0

        val vpl = ProfileManager.getInstance(this)

        var newname = possibleName

        // 	Default to
        if (mResult!!.mName != null && ConfigParser.CONVERTED_PROFILE != mResult!!.mName)
            newname = mResult!!.mName

        while (newname == null || vpl.getProfileByName(newname) != null) {
            i++
            if (i == 1)
                newname = getString(R.string.converted_profile)
            else
                newname = getString(R.string.converted_profile_i, i)
        }

        return newname
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.import_menu, menu)
        return true
    }

    private fun embedFile(filename: String?, type: Utils.FileType, onlyFindFileAndNullonNotFound: Boolean): String? {
        if (filename == null)
            return null

        // Already embedded, nothing to do
        if (VpnProfile.isEmbedded(filename))
            return filename

        val possibleFile = findFile(filename, type)
        return if (possibleFile == null)
            if (onlyFindFileAndNullonNotFound)
                null
            else
                filename
        else if (onlyFindFileAndNullonNotFound)
            possibleFile.absolutePath
        else
            readFileContent(possibleFile, type == Utils.FileType.PKCS12)

    }


    private fun getFileDialogInfo(type: Utils.FileType): Pair<Int, String> {
        var titleRes = 0
        var value: String? = null
        when (type) {
            Utils.FileType.KEYFILE -> {
                titleRes = R.string.client_key_title
                if (mResult != null)
                    value = mResult!!.mClientKeyFilename
            }
            Utils.FileType.CLIENT_CERTIFICATE -> {
                titleRes = R.string.client_certificate_title
                if (mResult != null)
                    value = mResult!!.mClientCertFilename
            }
            Utils.FileType.CA_CERTIFICATE -> {
                titleRes = R.string.ca_title
                if (mResult != null)
                    value = mResult!!.mCaFilename
            }
            Utils.FileType.TLS_AUTH_FILE -> {
                titleRes = R.string.tls_auth_file
                if (mResult != null)
                    value = mResult!!.mTLSAuthFilename
            }
            Utils.FileType.PKCS12 -> {
                titleRes = R.string.client_pkcs12_title
                if (mResult != null)
                    value = mResult!!.mPKCS12Filename
            }

            Utils.FileType.USERPW_FILE -> {
                titleRes = R.string.userpw_file
                value = mEmbeddedPwFile
            }

            Utils.FileType.CRL_FILE -> {
                titleRes = R.string.crl_file
                value = mResult!!.mCrlFilename
            }
            Utils.FileType.OVPN_CONFIG -> TODO()
        }

        return Pair.create(titleRes, value)

    }

    private fun findFile(filename: String?, fileType: Utils.FileType): File? {
        val foundfile = findFileRaw(filename)

        if (foundfile == null && filename != null && filename != "") {
            log(R.string.import_could_not_open, filename)
        }
        fileSelectMap[fileType] = null

        return foundfile
    }

    private fun addMissingFileDialogs() {
        for ((key, value) in fileSelectMap) {
            if (value == null)
                addFileSelectDialog(key)
        }
    }

    private fun addFileSelectDialog(type: Utils.FileType?) {

        val fileDialogInfo = getFileDialogInfo(type!!)

        val isCert = type == Utils.FileType.CA_CERTIFICATE || type == Utils.FileType.CLIENT_CERTIFICATE
        val fl = FileSelectLayout(this, getString(fileDialogInfo.first), isCert, false)
        fileSelectMap[type] = fl
        fl.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        (findViewById<View>(R.id.config_convert_root) as LinearLayout).addView(fl, 2)
        findViewById<View>(R.id.files_missing_hint).visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M)
            checkPermission()

        fl.setData(fileDialogInfo.second, this)
        val i = getFileLayoutOffset(type)
        fl.setCaller(this, i, type)

    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            findViewById<View>(R.id.permssion_hint).visibility = View.VISIBLE
            findViewById<View>(R.id.permssion_hint).setOnClickListener(this)
        }
    }

    private fun getFileLayoutOffset(type: Utils.FileType): Int {
        return CHOOSE_FILE_OFFSET + type.value
    }


    private fun findFileRaw(filename: String?): File? {
        if (filename == null || filename == "")
            return null

        // Try diffent path relative to /mnt/sdcard
        val sdcard = Environment.getExternalStorageDirectory()
        val root = File("/")

        val dirlist = HashSet<File>()

        for (i in mPathsegments!!.indices.reversed()) {
            var path = ""
            for (j in 0..i) {
                path += "/" + mPathsegments!![j]
            }
            // Do a little hackish dance for the Android File Importer
            // /document/primary:ovpn/openvpn-imt.conf


            if (path.indexOf(':') != -1 && path.lastIndexOf('/') > path.indexOf(':')) {
                var possibleDir = path.substring(path.indexOf(':') + 1, path.length)
                // Unquote chars in the  path
                try {
                    possibleDir = URLDecoder.decode(possibleDir, "UTF-8")
                } catch (ignored: UnsupportedEncodingException) {
                }

                possibleDir = possibleDir.substring(0, possibleDir.lastIndexOf('/'))




                dirlist.add(File(sdcard, possibleDir))

            }
            dirlist.add(File(path))


        }
        dirlist.add(sdcard)
        dirlist.add(root)


        val fileparts = filename.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (rootdir in dirlist) {
            var suffix = ""
            for (i in fileparts.indices.reversed()) {
                if (i == fileparts.size - 1)
                    suffix = fileparts[i]
                else
                    suffix = fileparts[i] + "/" + suffix

                val possibleFile = File(rootdir, suffix)
                if (possibleFile.canRead())
                    return possibleFile

            }
        }
        return null
    }

    internal fun readFileContent(possibleFile: File, base64encode: Boolean): String? {
        val filedata: ByteArray
        try {
            filedata = readBytesFromFile(possibleFile)
        } catch (e: IOException) {
            log(e.localizedMessage)
            return null
        }

        val data: String
        if (base64encode) {
            data = Base64.encodeToString(filedata, Base64.DEFAULT)
        } else {
            data = String(filedata)

        }

        return VpnProfile.DISPLAYNAME_TAG + possibleFile.name + VpnProfile.INLINE_TAG + data

    }


    @Throws(IOException::class)
    private fun readBytesFromFile(file: File): ByteArray {
        val input = FileInputStream(file)

        val len = file.length()
        if (len > VpnProfile.MAX_EMBED_FILE_SIZE)
            throw IOException("File size of file to import too large.")

        // Create the byte array to hold the data
        val bytes = ByteArray(len.toInt())

        // Read in the bytes
        var offset = 0
        var bytesRead: Int
        do {
            bytesRead = input.read(bytes, offset, bytes.size - offset)
            offset += bytesRead
        } while (offset < bytes.size && bytesRead >= 0)

        input.close()
        return bytes
    }

    internal fun embedFiles(cp: ConfigParser?) {
        // This where I would like to have a c++ style
        // void embedFile(std::string & option)

        if (mResult!!.mPKCS12Filename != null) {
            val pkcs12file = findFileRaw(mResult!!.mPKCS12Filename)
            if (pkcs12file != null) {
                mAliasName = pkcs12file.name.replace(".p12", "")
            } else {
                mAliasName = "Imported PKCS12"
            }
        }


        mResult!!.mCaFilename = embedFile(mResult!!.mCaFilename, Utils.FileType.CA_CERTIFICATE, false)
        mResult!!.mClientCertFilename = embedFile(mResult!!.mClientCertFilename, Utils.FileType.CLIENT_CERTIFICATE, false)
        mResult!!.mClientKeyFilename = embedFile(mResult!!.mClientKeyFilename, Utils.FileType.KEYFILE, false)
        mResult!!.mTLSAuthFilename = embedFile(mResult!!.mTLSAuthFilename, Utils.FileType.TLS_AUTH_FILE, false)
        mResult!!.mPKCS12Filename = embedFile(mResult!!.mPKCS12Filename, Utils.FileType.PKCS12, false)
        mResult!!.mCrlFilename = embedFile(mResult!!.mCrlFilename, Utils.FileType.CRL_FILE, true)
        if (cp != null) {
            mEmbeddedPwFile = cp.authUserPassFile
            mEmbeddedPwFile = embedFile(cp.authUserPassFile, Utils.FileType.USERPW_FILE, false)
        }

    }

    private fun updateFileSelectDialogs() {
        for ((key, value) in fileSelectMap) {
            value?.setData(getFileDialogInfo(key).second, this)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.config_converter)

        val fab_button = findViewById<ImageButton?>(R.id.fab_save)
        if (fab_button != null) {
            fab_button.setOnClickListener(this)
            findViewById<View>(R.id.fab_footerspace).visibility = View.VISIBLE
        }

        mLogLayout = findViewById<View>(R.id.config_convert_root) as LinearLayout


        mProfilename = findViewById<View>(R.id.profilename) as EditText
        mProfilenameLabel = findViewById<View>(R.id.profilename_label) as TextView

        if (savedInstanceState != null && savedInstanceState.containsKey(VPNPROFILE)) {
            mResult = savedInstanceState.getSerializable(VPNPROFILE) as VpnProfile?
            mAliasName = savedInstanceState.getString("mAliasName")
            mEmbeddedPwFile = savedInstanceState.getString("pwfile")
            mSourceUri = savedInstanceState.getParcelable("mSourceUri")
            mProfilename.setText(mResult!!.mName)

            if (savedInstanceState.containsKey("logentries")) {

                for (logItem in savedInstanceState.getStringArray("logentries")!!)
                    log(logItem)
            }
            if (savedInstanceState.containsKey("fileselects")) {

                for (k in savedInstanceState.getIntArray("fileselects")!!) {
                    addFileSelectDialog(Utils.FileType.getFileTypeByValue(k))
                }
            }
            return
        }


        if (intent != null) {
            doImportIntent(intent)

            // We parsed the intent, relay on saved instance for restoring
            setIntent(null)
        }


    }

    private fun doImportIntent(intent: Intent) {
        if (intent.action.equals(IMPORT_PROFILE_DATA)) {
            val data = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (data != null) {
                startImportTask(Uri.fromParts("inline", "inlinetext", null),
                        "imported profiles from AS", data);
            }
        } else if (intent.action.equals(IMPORT_PROFILE) || intent.action.equals(Intent.ACTION_VIEW)) {
            val data = intent.data
            if (data != null) {
                mSourceUri = data
                doImportUri(data)

            }
        }
    }

    private fun doImportUri(data: Uri) {
        //log(R.string.import_experimental);
        log(R.string.importing_config, data.toString())
        var possibleName: String? = null
        if (data.scheme != null && data.scheme == "file" || data.lastPathSegment != null && (data.lastPathSegment!!.endsWith(".ovpn") || data.lastPathSegment!!.endsWith(".conf"))) {
            possibleName = data.lastPathSegment
            if (possibleName!!.lastIndexOf('/') != -1)
                possibleName = possibleName.substring(possibleName.lastIndexOf('/') + 1)

        }

        mPathsegments = data.pathSegments

        val cursor = contentResolver.query(data, null, null, null, null)

        try {

            if (cursor != null && cursor.moveToFirst()) {
                var columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (columnIndex != -1) {
                    val displayName = cursor.getString(columnIndex)
                    if (displayName != null)
                        possibleName = displayName
                }
                columnIndex = cursor.getColumnIndex("mime_type")
                if (columnIndex != -1) {
                    log("Mime type: " + cursor.getString(columnIndex))
                }
            }
        } finally {
            cursor?.close()
        }
        if (possibleName != null) {
            possibleName = possibleName.replace(".ovpn", "")
            possibleName = possibleName.replace(".conf", "")
        }

        startImportTask(data, possibleName, "")


    }

    private fun startImportTask(data: Uri, possibleName: String?, inlineData: String) {
        mImportTask = object : AsyncTask<Void, Void, Int>() {
            private var mProgress: ProgressBar? = null

            override fun onPreExecute() {
                mProgress = ProgressBar(this@ConfigConverter)
                addViewToLog(mProgress)
            }

            override fun doInBackground(vararg params: Void): Int? {
                try {
                    var inputStream: InputStream?
                    if (data.scheme.equals("inline")) {
                        inputStream = inlineData.byteInputStream()
                    } else {
                        inputStream = contentResolver.openInputStream(data)
                    }

                    if (inputStream != null) {
                        doImport(inputStream)
                    }
                    if (mResult == null)
                        return -3
                } catch (se: IOException) {
                    log(R.string.import_content_resolve_error.toString() + ":" + se.localizedMessage)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        checkMarschmallowFileImportError(data)
                    return -2
                } catch (se: SecurityException) {
                    log(R.string.import_content_resolve_error.toString() + ":" + se.localizedMessage)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        checkMarschmallowFileImportError(data)
                    return -2
                }

                return 0
            }

            override fun onPostExecute(errorCode: Int?) {
                mLogLayout.removeView(mProgress)
                addMissingFileDialogs()
                updateFileSelectDialogs()

                if (errorCode == 0) {
                    displayWarnings()
                    mResult!!.mName = getUniqueProfileName(possibleName)
                    mProfilename.visibility = View.VISIBLE
                    mProfilenameLabel.visibility = View.VISIBLE
                    mProfilename.setText(mResult!!.name)

                    log(R.string.import_done)
                }
            }
        }.execute()
    }


    @TargetApi(Build.VERSION_CODES.M)
    private fun checkMarschmallowFileImportError(data: Uri?) {
        // Permission already granted, not the source of the error
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            return

        // We got a file:/// URL and have no permission to read it. Technically an error of the calling app since
        // it makes an assumption about other apps being able to read the url but well ...
        if (data != null && "file" == data.scheme)
            doRequestSDCardPermission(PERMISSION_REQUEST_READ_URL)

    }


    override fun onStart() {
        super.onStart()
    }

    private fun log(logmessage: String?) {
        runOnUiThread {
            val tv = TextView(this@ConfigConverter)
            mLogEntries.add(logmessage)
            tv.text = logmessage

            addViewToLog(tv)
        }
    }

    private fun addViewToLog(view: View?) {
        mLogLayout.addView(view, mLogLayout.childCount - 1)
    }

    private fun doImport(inputStream: InputStream) {
        val cp = ConfigParser()
        try {
            val isr = InputStreamReader(inputStream)

            cp.parseConfig(isr)
            mResult = cp.convertProfile()
            embedFiles(cp)
            return

        } catch (e: IOException) {
            log(R.string.error_reading_config_file)
            log(e.localizedMessage)
        } catch (e: ConfigParseError) {
            log(R.string.error_reading_config_file)
            log(e.localizedMessage)
        } finally {
            inputStream.close()
        }

        mResult = null
    }


    private fun displayWarnings() {
        if (mResult!!.mUseCustomConfig) {
            log(R.string.import_warning_custom_options)
            var copt = mResult!!.mCustomConfigOptions
            if (copt.startsWith("#")) {
                val until = copt.indexOf('\n')
                copt = copt.substring(until + 1)
            }

            log(copt)
        }

        if (mResult!!.mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mResult!!.mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
            findViewById<View>(R.id.importpkcs12).visibility = View.VISIBLE
        }

    }

    private fun log(ressourceId: Int, vararg formatArgs: Any) {
        log(getString(ressourceId, *formatArgs))
    }

    companion object {

        @kotlin.jvm.JvmField
        val IMPORT_PROFILE = "de.blinkt.openvpn.IMPORT_PROFILE"
        val IMPORT_PROFILE_DATA = "de.blinkt.openvpn.IMPORT_PROFILE_DATA"
        private val RESULT_INSTALLPKCS12 = 7
        private val CHOOSE_FILE_OFFSET = 1000
        val VPNPROFILE = "vpnProfile"
        private val PERMISSION_REQUEST_EMBED_FILES = 37231
        private val PERMISSION_REQUEST_READ_URL = PERMISSION_REQUEST_EMBED_FILES + 1
    }

}
