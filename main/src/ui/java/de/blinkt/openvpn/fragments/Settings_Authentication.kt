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
import androidx.activity.result.ActivityResultLauncher
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

class Settings_Authentication : OpenVpnPreferencesFragment(), Preference.OnPreferenceClickListener {
    private lateinit var handleFileSelectResult: ActivityResultLauncher<Intent>
    private lateinit var handleSystemChooserResult: ActivityResultLauncher<Intent>
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
    private lateinit var mTLSProfile: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.vpn_authentification)
        mExpectTLSCert = findPreference("remoteServerTLS")!!
        mCheckRemoteCN = findPreference("checkRemoteCN")!!
        mRemoteCN = findPreference("remotecn")!!
        setRemoteCNSummaryProvider()
        mRemoteX509Name = findPreference("remotex509name")!!
        mRemoteX509Name.setSummaryProvider { pref ->
            if ((pref as EditTextPreference).text?.isEmpty() == true) "CN (default)" else pref.text
        }
        mUseTLSAuth = findPreference("useTLSAuth")!!
        mTLSAuthFile = findPreference("tlsAuthFile")!!
        mTLSAuthFile.onPreferenceClickListener = this
        mTLSAuthDirection = findPreference("tls_direction")!!
        mDataCiphers = findPreference("dataciphers")!!
        mAuth = findPreference("auth")!!
        mTLSProfile = findPreference("tls_profile")!!

        createActivityResultHandlers()
        loadSettings()
    }

    override fun loadSettings() {
        if (!this::mExpectTLSCert.isInitialized) {
            return;
        }
        mExpectTLSCert.isChecked = mProfile.mExpectTLSCert
        mCheckRemoteCN.isChecked = mProfile.mCheckRemoteCN
        mRemoteCN.setDN(mProfile.mRemoteCN)
        mRemoteCN.setAuthType(mProfile.mX509AuthType)
        mRemoteX509Name.text = mProfile.mx509UsernameField ?: ""
        mUseTLSAuth.isChecked = mProfile.mUseTLSAuth

        mTlsAuthFileData = mProfile.mTLSAuthFilename
        setTlsAuthSummary(mTlsAuthFileData)

        mTLSAuthDirection.value = mProfile.mTLSAuthDirection

        mDataCiphers.text = mProfile.mDataCiphers
        mAuth.text = mProfile.mAuth

        if (mProfile.mAuthenticationType == VpnProfile.TYPE_STATICKEYS) {
            mExpectTLSCert.isEnabled = false
            mCheckRemoteCN.isEnabled = false
            mUseTLSAuth.isChecked = true
        } else {
            mExpectTLSCert.isEnabled = true
            mCheckRemoteCN.isEnabled = true
        }
        if (mProfile.mTlSCertProfile.isNullOrBlank())
            mTLSProfile.value = "legacy"
        else
            mTLSProfile.value = mProfile.mTlSCertProfile
    }

    override fun saveSettings() {
        mProfile.mExpectTLSCert = mExpectTLSCert.isChecked
        mProfile.mCheckRemoteCN = mCheckRemoteCN.isChecked
        mProfile.mRemoteCN = mRemoteCN.cnText
        mProfile.mX509AuthType = mRemoteCN.authtype
        mProfile.mUseTLSAuth = mUseTLSAuth.isChecked
        mProfile.mTLSAuthFilename = mTlsAuthFileData
        mProfile.mx509UsernameField = mRemoteX509Name.text
        mProfile.mTLSAuthDirection = mTLSAuthDirection.value
        mProfile.mDataCiphers = mDataCiphers.text
        mProfile.mAuth = mAuth.text
        mProfile.mTlSCertProfile = mTLSProfile.value
    }

    private fun setRemoteCNSummaryProvider()
    {
        mRemoteCN.setSummaryProvider {
            pref ->
            pref as RemoteCNPreference;

            if ("" == pref.cnText) {
                if (mProfile.mConnections.size > 0) {
                    return@setSummaryProvider getX509String(VpnProfile.X509_VERIFY_TLSREMOTE_RDN,
                        mProfile.mConnections[0].mServerName
                    )
                } else {
                    return@setSummaryProvider getString(R.string.no_remote_defined)
                }
            } else {
                return@setSummaryProvider getX509String(pref.authtype, pref.cnText)
            }
        }
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

    fun createActivityResultHandlers()
    {
        handleSystemChooserResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
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
        handleFileSelectResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resData = result.data?.getStringExtra(FileSelect.RESULT_DATA)
            mTlsAuthFileData = resData
            setTlsAuthSummary(resData)
        }
    }

    fun startFileDialog() {
        var startFC: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !alwaysUseOldFileChooser(activity)) {
            startFC = getFilePickerIntent(requireContext(), Utils.FileType.TLS_AUTH_FILE)
            if (startFC != null)
                handleSystemChooserResult.launch(startFC)
        }
        if (startFC == null) {
            startFC = Intent(activity, FileSelect::class.java)
            startFC.putExtra(FileSelect.START_DATA, mTlsAuthFileData)
            startFC.putExtra(FileSelect.WINDOW_TITLE, R.string.tls_auth_file)
            handleFileSelectResult.launch(startFC)
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