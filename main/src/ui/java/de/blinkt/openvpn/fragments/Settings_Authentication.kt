/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.fragments

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Pair
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.preference.*
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.activities.FileSelect
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.fragments.Utils.alwaysUseOldFileChooser
import de.blinkt.openvpn.fragments.Utils.getFilePickerIntent
import de.blinkt.openvpn.fragments.Utils.getFilePickerResult
import de.blinkt.openvpn.views.RemoteCNPreference
import de.blinkt.openvpn.views.RemoteCNPreferenceDialog
import java.io.IOException

class Settings_Authentication : OpenVpnPreferencesFragment(), Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {
    private lateinit var mExpectTLSCert: CheckBoxPreference
    private lateinit var mCheckRemoteCN: CheckBoxPreference
    private lateinit var mRemoteCN: RemoteCNPreference
    private lateinit var mTLSAuthDirection: ListPreference
    private lateinit var mTLSAuthFile: Preference
    private lateinit var mUseTLSAuth: SwitchPreference
    private lateinit var mDataCiphers: EditTextPreference
    private var mTlsAuthFileData: String? = null
    private lateinit var mAuth: EditTextPreference
    private lateinit var mRemoteX509Name: EditTextPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.vpn_authentification)
        mExpectTLSCert = findPreference("remoteServerTLS")!!
        mCheckRemoteCN = findPreference("checkRemoteCN")!!
        mRemoteCN = findPreference("remotecn")!!
        mRemoteCN.onPreferenceChangeListener = this
        mRemoteX509Name = findPreference("remotex509name")!!
        mRemoteX509Name.onPreferenceChangeListener = this
        mUseTLSAuth = findPreference("useTLSAuth")!!
        mTLSAuthFile = findPreference("tlsAuthFile")!!
        mTLSAuthDirection = findPreference("tls_direction")!!
        mTLSAuthFile.onPreferenceClickListener = this
        mDataCiphers = findPreference("dataciphers")!!
        mDataCiphers.onPreferenceChangeListener = this
        mAuth = findPreference("auth")!!
        mAuth.onPreferenceChangeListener = this
        loadSettings()
    }

    override fun loadSettings() {
        mExpectTLSCert.isChecked = mProfile.mExpectTLSCert
        mCheckRemoteCN.isChecked = mProfile.mCheckRemoteCN
        mRemoteCN.setDN(mProfile.mRemoteCN)
        mRemoteCN.setAuthType(mProfile.mX509AuthType)
        onPreferenceChange(
            mRemoteCN,
            Pair(mProfile.mX509AuthType, mProfile.mRemoteCN)
        )
        mRemoteX509Name.text = mProfile.mx509UsernameField ?: ""
        onPreferenceChange(mRemoteX509Name, mProfile.mx509UsernameField ?: "")
        mUseTLSAuth.isChecked = mProfile.mUseTLSAuth
        mTlsAuthFileData = mProfile.mTLSAuthFilename
        setTlsAuthSummary(mTlsAuthFileData)
        mTLSAuthDirection.value = mProfile.mTLSAuthDirection
        mDataCiphers.text = mProfile.mDataCiphers
        onPreferenceChange(mDataCiphers, mProfile.mDataCiphers)
        mAuth.text = mProfile.mAuth
        onPreferenceChange(mAuth, mProfile.mAuth)
        if (mProfile.mAuthenticationType == VpnProfile.TYPE_STATICKEYS) {
            mExpectTLSCert.isEnabled = false
            mCheckRemoteCN.isEnabled = false
            mUseTLSAuth.isChecked = true
        } else {
            mExpectTLSCert.isEnabled = true
            mCheckRemoteCN.isEnabled = true
        }
    }

    override fun saveSettings() {
        mProfile.mExpectTLSCert = mExpectTLSCert.isChecked
        mProfile.mCheckRemoteCN = mCheckRemoteCN.isChecked
        mProfile.mRemoteCN = mRemoteCN.cnText
        mProfile.mX509AuthType = mRemoteCN.authtype
        mProfile.mUseTLSAuth = mUseTLSAuth.isChecked
        mProfile.mTLSAuthFilename = mTlsAuthFileData
        mProfile.mx509UsernameField = mRemoteX509Name.text
        if (mTLSAuthDirection.value == null) mProfile.mTLSAuthDirection =
            null else mProfile.mTLSAuthDirection = mTLSAuthDirection.value
        if (mDataCiphers.text == null) mProfile.mDataCiphers = null else mProfile.mDataCiphers =
            mDataCiphers.text
        if (mAuth.text == null) mProfile.mAuth = null else mProfile.mAuth = mAuth.text
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === mRemoteCN && newValue is Pair<*, *>) {
            val authtype = newValue.first as Int
            val dn = newValue.second
            if ("" == dn) {
                if (mProfile.mConnections.size > 0) {
                    preference.summary = getX509String(
                        VpnProfile.X509_VERIFY_TLSREMOTE_RDN,
                        mProfile.mConnections[0].mServerName
                    )
                } else {
                    preference.setSummary(R.string.no_remote_defined)
                }
            } else {
                preference.summary = getX509String(authtype, dn as String)
            }
        } else if (preference === mDataCiphers || preference === mAuth) {
            preference.summary = (newValue as CharSequence)
        } else if (preference === mRemoteX509Name) {
            preference.summary =
                (if (newValue.toString().isEmpty()) "CN (default)" else newValue as CharSequence)
        }
        return true
    }

    private fun getX509String(authtype: Int, dn: String): CharSequence {
        var ret = ""
        when (authtype) {
            VpnProfile.X509_VERIFY_TLSREMOTE, VpnProfile.X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING -> ret += "tls-remote "
            VpnProfile.X509_VERIFY_TLSREMOTE_DN -> ret = "dn: "
            VpnProfile.X509_VERIFY_TLSREMOTE_RDN -> ret = "rdn: "
            VpnProfile.X509_VERIFY_TLSREMOTE_RDN_PREFIX -> ret = "rdn prefix: "
        }
        return ret + dn
    }

    fun startFileDialog() {
        var startFC: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !alwaysUseOldFileChooser(activity)) {
            startFC = getFilePickerIntent(requireContext(), Utils.FileType.TLS_AUTH_FILE)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK)
                    try {
                        mTlsAuthFileData =
                            getFilePickerResult(
                                Utils.FileType.TLS_AUTH_FILE,
                                result.data,
                                requireContext()
                            )
                        setTlsAuthSummary(mTlsAuthFileData)
                    } catch (e: IOException) {
                        VpnStatus.logException(e)
                    } catch (se: SecurityException) {
                        VpnStatus.logException(se)
                    }
            }
        }
        if (startFC == null) {
            startFC = Intent(activity, FileSelect::class.java)
            startFC.putExtra(FileSelect.START_DATA, mTlsAuthFileData)
            startFC.putExtra(FileSelect.WINDOW_TITLE, R.string.tls_auth_file)
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

                val resData = result.data?.getStringExtra(FileSelect.RESULT_DATA)
                mTlsAuthFileData = resData
                setTlsAuthSummary(resData)

            }
        }
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        startFileDialog()
        return true
    }


    private fun setTlsAuthSummary(result: String?) {
        var result = result
        if (result == null)
            result = getString(R.string.no_certificate)

        if (result.startsWith(VpnProfile.INLINE_TAG))
            mTLSAuthFile.setSummary(R.string.inline_file_data)
        else if (result.startsWith(VpnProfile.DISPLAYNAME_TAG))
            mTLSAuthFile.summary =
                getString(R.string.imported_from_file, VpnProfile.getDisplayName(result))
        else
            mTLSAuthFile.summary = result
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        var dialogFragment: DialogFragment? = null
        if (preference is RemoteCNPreference) {
            dialogFragment = RemoteCNPreferenceDialog.newInstance(preference.getKey())
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0)
            dialogFragment.show(parentFragmentManager, "RemoteCNDialog")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }
}