/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.security.KeyChain
import android.security.KeyChainException
import android.security.keystore.KeyInfo
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.api.ExternalCertificateProvider
import de.blinkt.openvpn.core.ExtAuthHelper
import de.blinkt.openvpn.core.X509Utils
import java.security.KeyFactory
import java.security.PrivateKey

import java.security.cert.X509Certificate

internal abstract class KeyChainSettingsFragment : Settings_Fragment(), View.OnClickListener, Handler.Callback {


    private lateinit var mAliasCertificate: TextView
    private lateinit var mAliasName: TextView
    private var mHandler: Handler? = null
    private lateinit var mExtAliasName: TextView
    private lateinit var mExtAuthSpinner: Spinner

    private val isInHardwareKeystore: Boolean
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Throws(KeyChainException::class, InterruptedException::class)
        get() {
            val key: PrivateKey = KeyChain.getPrivateKey(activity!!.applicationContext, mProfile.mAlias) ?: return false

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                val keyFactory = KeyFactory.getInstance(key.getAlgorithm(), "AndroidKeyStore")
                val keyInfo = keyFactory.getKeySpec(key, KeyInfo::class.java)
                return keyInfo.isInsideSecureHardware()

            } else {
                val algorithm = key.algorithm
                return KeyChain.isBoundKeyAlgorithm(algorithm)
            }
        }


    private fun setKeyStoreAlias() {
        if (mProfile.mAlias == null) {
            mAliasName.setText(R.string.client_no_certificate)
            mAliasCertificate.text = ""
        } else {
            mAliasCertificate.text = "Loading certificate from Keystore..."
            mAliasName.text = mProfile.mAlias
            setCertificate(false)
        }
    }

    private fun setExtAlias() {
        if (mProfile.mAlias == null) {
            mExtAliasName.setText(R.string.extauth_not_configured)
            mAliasCertificate.text = ""
        } else {
            mAliasCertificate.text = "Querying certificate from external provider..."
            mExtAliasName.text = ""
            setCertificate(true)
        }
    }

    private fun fetchExtCertificateMetaData() {
        object : Thread() {
            override fun run() {
                try {
                    val b = ExtAuthHelper.getCertificateMetaData(context!!, mProfile.mExternalAuthenticator, mProfile.mAlias)
                    mProfile.mAlias = b.getString(ExtAuthHelper.EXTRA_ALIAS)
                    activity!!.runOnUiThread { setAlias() }
                } catch (e: KeyChainException) {
                    e.printStackTrace()
                }

            }
        }.start()
    }


    protected fun setCertificate(external: Boolean) {
        object : Thread() {
            override fun run() {
                var certstr = ""
                var metadata: Bundle? = null
                try {
                    var cert: X509Certificate? = null

                    if (external) {
                        if (!TextUtils.isEmpty(mProfile.mExternalAuthenticator) && !TextUtils.isEmpty(mProfile.mAlias)) {
                            cert = ExtAuthHelper.getCertificateChain(context!!, mProfile.mExternalAuthenticator, mProfile.mAlias)!![0]
                            metadata = ExtAuthHelper.getCertificateMetaData(context!!, mProfile.mExternalAuthenticator, mProfile.mAlias)
                        } else {
                            cert = null
                            certstr = getString(R.string.extauth_not_configured)
                        }
                    } else {
                        val certChain = KeyChain.getCertificateChain(activity!!.applicationContext, mProfile.mAlias)
                        if (certChain != null) {
                            cert = certChain[0]
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                run {
                                    if (isInHardwareKeystore)
                                        certstr += getString(R.string.hwkeychain)
                                }
                            }
                        }
                    }
                    if (cert != null) {
                        certstr += X509Utils.getCertificateValidityString(cert, resources)
                        certstr += X509Utils.getCertificateFriendlyName(cert)
                    }

                } catch (e: Exception) {
                    certstr = "Could not get certificate from Keystore: " + e.localizedMessage
                }

                val certStringCopy = certstr
                val finalMetadata = metadata
                activity!!.runOnUiThread {
                    mAliasCertificate.text = certStringCopy
                    if (finalMetadata != null)
                        mExtAliasName.text = finalMetadata.getString(ExtAuthHelper.EXTRA_DESCRIPTION)

                }

            }
        }.start()
    }

    protected fun initKeychainViews(v: View) {
        v.findViewById<View>(R.id.select_keystore_button).setOnClickListener(this)
        v.findViewById<View>(R.id.configure_extauth_button)?.setOnClickListener(this)
        v.findViewById<View>(R.id.install_keystore_button).setOnClickListener(this)
        mAliasCertificate = v.findViewById(R.id.alias_certificate)
        mExtAuthSpinner = v.findViewById(R.id.extauth_spinner)
        mExtAuthSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedProvider = parent.getItemAtPosition(position) as ExtAuthHelper.ExternalAuthProvider
                if (selectedProvider.packageName != mProfile.mExternalAuthenticator) {
                    mProfile.mAlias = ""
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        mExtAliasName = v.findViewById(R.id.extauth_detail)
        mAliasName = v.findViewById(R.id.aliasname)
        if (mHandler == null) {
            mHandler = Handler(this)
        }
        ExtAuthHelper.setExternalAuthProviderSpinnerList(mExtAuthSpinner, mProfile.mExternalAuthenticator)

        v.findViewById<View>(R.id.install_keystore_button).setOnClickListener {
            startActivity(KeyChain.createInstallIntent())
        };
    }

    override fun onClick(v: View) {
        if (v === v.findViewById<View>(R.id.select_keystore_button)) {
            showCertDialog()
        } else if (v === v.findViewById<View>(R.id.configure_extauth_button)) {
            startExternalAuthConfig()
        }
    }

    private fun startExternalAuthConfig() {
        val eAuth = mExtAuthSpinner.selectedItem as ExtAuthHelper.ExternalAuthProvider
        mProfile.mExternalAuthenticator = eAuth.packageName
        if (!eAuth.configurable) {
            fetchExtCertificateMetaData()
            return
        }
        val extauth = Intent(ExtAuthHelper.ACTION_CERT_CONFIGURATION)
        extauth.setPackage(eAuth.packageName)
        extauth.putExtra(ExtAuthHelper.EXTRA_ALIAS, mProfile.mAlias)
        startActivityForResult(extauth, UPDATEE_EXT_ALIAS)
    }

    override fun savePreferences() {

    }

    override fun onStart() {
        super.onStart()
        loadPreferences()
    }

    fun showCertDialog() {
        try {
            KeyChain.choosePrivateKeyAlias(activity!!,
                    { alias ->
                        // Credential alias selected.  Remember the alias selection for future use.
                        mProfile.mAlias = alias
                        mHandler!!.sendEmptyMessage(UPDATE_ALIAS)
                    },
                    arrayOf("RSA", "EC"), null, // issuer, null for any
                    mProfile.mServerName, // host name of server requesting the cert, null if unavailable
                    -1, // port of server requesting the cert, -1 if unavailable
                    mProfile.mAlias)// List of acceptable key types. null for any
            // alias to preselect, null if unavailable
        } catch (anf: ActivityNotFoundException) {
            val ab = AlertDialog.Builder(activity)
            ab.setTitle(R.string.broken_image_cert_title)
            ab.setMessage(R.string.broken_image_cert)
            ab.setPositiveButton(android.R.string.ok, null)
            ab.show()
        }

    }

    protected open fun loadPreferences() {
        setAlias()

    }

    private fun setAlias() {
        if (mProfile.mAuthenticationType == VpnProfile.TYPE_EXTERNAL_APP)
            setExtAlias()
        else
            setKeyStoreAlias()
    }

    override fun handleMessage(msg: Message): Boolean {
        setAlias()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null && requestCode == UPDATEE_EXT_ALIAS && resultCode == Activity.RESULT_OK) {
            mProfile.mAlias = data.getStringExtra(ExtAuthHelper.EXTRA_ALIAS)
            mExtAliasName.text = data.getStringExtra(ExtAuthHelper.EXTRA_DESCRIPTION)
        }
    }

    companion object {
        private val UPDATE_ALIAS = 20
        private val UPDATEE_EXT_ALIAS = 210
    }
}
