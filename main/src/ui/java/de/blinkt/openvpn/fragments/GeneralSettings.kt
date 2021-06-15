/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.fragments

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import de.blinkt.openvpn.BuildConfig
import de.blinkt.openvpn.R
import de.blinkt.openvpn.activities.OpenSSLSpeed
import de.blinkt.openvpn.api.ExternalAppDatabase
import de.blinkt.openvpn.core.ProfileManager
import java.io.File

class GeneralSettings : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener,
    DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener {
    private lateinit var mExtapp: ExternalAppDatabase
    private lateinit var mAlwaysOnVPN: ListPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_settings)
        val devHacks = findPreference<PreferenceCategory>("device_hacks") as PreferenceCategory
        mAlwaysOnVPN = findPreference("alwaysOnVpn")!!
        mAlwaysOnVPN.onPreferenceChangeListener = this
        val loadtun = findPreference<Preference>("loadTunModule")!!
        if (!isTunModuleAvailable) {
            loadtun.isEnabled = false
            devHacks.removePreference(loadtun)
        }
        val cm9hack = findPreference<Preference>("useCM9Fix") as CheckBoxPreference
        if (!cm9hack.isChecked && Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            devHacks.removePreference(cm9hack)
        }
        val useInternalFS =
            findPreference<Preference>("useInternalFileSelector") as CheckBoxPreference
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            devHacks.removePreference(useInternalFS)
        }

        /* Android P does not allow access to the file storage anymore */if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val useInternalFileSelector = findPreference<Preference>("useInternalFileSelector")
            devHacks.removePreference(useInternalFileSelector)
        }
        mExtapp = ExternalAppDatabase(activity)
        val clearapi = findPreference<Preference>("clearapi") as Preference
        clearapi.onPreferenceClickListener = this
        findPreference<Preference>("osslspeed")!!.onPreferenceClickListener = this
        if (devHacks.preferenceCount == 0) preferenceScreen.removePreference(devHacks)
        if (!BuildConfig.openvpn3) {
            val appBehaviour = findPreference<Preference>("app_behaviour") as PreferenceCategory
            val ovpn3 = findPreference<Preference>("ovpn3") as CheckBoxPreference
            ovpn3.isEnabled = false
            ovpn3.isChecked = false
        }

        val startOnBoot =
            findPreference<Preference>("restartvpnonboot") as CheckBoxPreference

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val vpn:VpnService = VpnService()
            startOnBoot.isChecked = vpn.isAlwaysOn
        }

        startOnBoot.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { pref: Preference?, newValue: Any ->
                if (newValue == true) {
                    val vpn = ProfileManager.getAlwaysOnVPN(requireActivity())
                    if (vpn == null) {
                        Toast.makeText(
                            requireContext(),
                            R.string.no_default_vpn_set,
                            Toast.LENGTH_LONG
                        ).show()
                        return@OnPreferenceChangeListener false
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && newValue == true)
                {
                    val builder = AlertDialog.Builder(
                        requireContext()
                    )
                    builder.setMessage(R.string.use_alwayson_vpn)
                    builder.setNeutralButton(R.string.open_vpn_settings, { dialog, which ->
                        startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                    })
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.show()
                }
                true
            }

        setClearApiSummary()
    }

    override fun onResume() {
        super.onResume()
        val vpn = ProfileManager.getAlwaysOnVPN(activity)
        val sb = StringBuffer(getString(R.string.defaultvpnsummary))
        sb.append('\n')
        if (vpn == null) sb.append(getString(R.string.novpn_selected)) else sb.append(
            getString(
                R.string.vpnselected,
                vpn.name
            )
        )
        mAlwaysOnVPN.summary = sb.toString()
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === mAlwaysOnVPN) {
            val vpn = ProfileManager.get(activity, newValue as String)
            mAlwaysOnVPN.summary = vpn.name
        }
        return true
    }

    private fun setClearApiSummary() {
        val clearapi = findPreference<Preference>("clearapi") as Preference
        if (mExtapp.extAppList.isEmpty()) {
            clearapi.isEnabled = false
            clearapi.setSummary(R.string.no_external_app_allowed)
        } else {
            clearapi.isEnabled = true
            clearapi.summary = getString(R.string.allowed_apps, getExtAppList(", "))
        }
    }

    private fun getExtAppList(delim: String): String {
        var app: ApplicationInfo
        val pm = requireActivity().packageManager
        val applist = StringBuilder()
        for (packagename in mExtapp.extAppList) {
            try {
                app = pm.getApplicationInfo(packagename, 0)
                if (applist.length != 0) applist.append(delim)
                applist.append(app.loadLabel(pm))
            } catch (e: PackageManager.NameNotFoundException) {
                // App not found. Remove it from the list
                mExtapp.removeApp(packagename)
            }
        }
        return applist.toString()
    }

    // Check if the tun module exists on the file system
    private val isTunModuleAvailable: Boolean
        get() =// Check if the tun module exists on the file system
            File("/system/lib/modules/tun.ko").length() > 10

    override fun onPreferenceClick(preference: Preference): Boolean {
        if (preference.key == "clearapi") {
            val builder = AlertDialog.Builder(
                requireContext()
            )
            builder.setPositiveButton(R.string.clear, this)
            builder.setNegativeButton(android.R.string.cancel, null)
            builder.setMessage(getString(R.string.clearappsdialog, getExtAppList("\n")))
            builder.show()
        } else if (preference.key == "osslspeed") {
            startActivity(Intent(activity, OpenSSLSpeed::class.java))
        }
        return true
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == Dialog.BUTTON_POSITIVE) {
            mExtapp.clearAllApiApps()
            setClearApiSummary()
        }
    }
}