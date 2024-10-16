/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.fragments

import android.os.Bundle
import android.view.View
import androidx.preference.*
import de.blinkt.openvpn.R
import de.blinkt.openvpn.fragments.OpenVpnPreferencesFragment
import de.blinkt.openvpn.VpnProfile

class Settings_IP : OpenVpnPreferencesFragment(), Preference.OnPreferenceChangeListener {
    private lateinit var mIPv4: EditTextPreference
    private lateinit var mIPv6: EditTextPreference
    private lateinit var mUsePull: SwitchPreference
    private lateinit var mOverrideDNS: CheckBoxPreference
    private lateinit var mSearchdomain: EditTextPreference
    private lateinit var mDNS1: EditTextPreference
    private lateinit var mDNS2: EditTextPreference
    private lateinit var mNobind: CheckBoxPreference
    override fun onResume() {
        super.onResume()


        // Make sure default values are applied.  In a real app, you would
        // want this in a shared function that is used to retrieve the
        // SharedPreferences wherever they are needed.
        PreferenceManager.setDefaultValues(
            requireActivity(),
            R.xml.vpn_ipsettings, false
        )

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.vpn_ipsettings)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /* Bind the preferences early to avoid loadingSetting which is called
         * from the superclass to access an uninitialised earlyinit property
         */
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onBindPreferences() {
        super.onBindPreferences()
        bindPreferences()
        loadSettings()
    }

    private fun bindPreferences() {
        mIPv4 = findPreference("ipv4_address")!!
        mIPv6 = findPreference("ipv6_address")!!
        mUsePull = findPreference("usePull")!!
        mOverrideDNS = findPreference("overrideDNS")!!
        mSearchdomain = findPreference("searchdomain")!!
        mDNS1 = findPreference("dns1")!!
        mDNS2 = findPreference("dns2")!!
        mDNS1.summaryProvider = DNSSummaryProvider()
        mDNS2.summaryProvider = DNSSummaryProvider()
        mNobind = findPreference("nobind")!!
        mUsePull.onPreferenceChangeListener = this
        mOverrideDNS.onPreferenceChangeListener = this
    }


    override fun loadSettings() {
        // Since we maybe not have preferences bound yet, check if we actually have them bound.
        if (!this::mUsePull.isInitialized) {
            return;
        }
        if (mProfile.mAuthenticationType == VpnProfile.TYPE_STATICKEYS) mUsePull.isEnabled =
            false else mUsePull.isChecked = mProfile.mUsePull
        mIPv4.text = mProfile.mIPv4Address
        mIPv6.text = mProfile.mIPv6Address
        mDNS1.text = mProfile.mDNS1
        mDNS2.text = mProfile.mDNS2
        mOverrideDNS.isChecked = mProfile.mOverrideDNS
        mSearchdomain.text = mProfile.mSearchDomain
        mNobind.isChecked = mProfile.mNobind


        //mUsePull.setEnabled(mProfile.mAuthenticationType != VpnProfile.TYPE_STATICKEYS);
        mUsePull.isEnabled = true
        if (mProfile.mAuthenticationType == VpnProfile.TYPE_STATICKEYS) mUsePull.isChecked = false

        // Sets Summary
        setDNSState()
    }

    override fun saveSettings() {
        mProfile.mUsePull = mUsePull.isChecked
        mProfile.mIPv4Address = mIPv4.text
        mProfile.mIPv6Address = mIPv6.text
        mProfile.mDNS1 = mDNS1.text
        mProfile.mDNS2 = mDNS2.text
        mProfile.mOverrideDNS = mOverrideDNS.isChecked
        mProfile.mSearchDomain = mSearchdomain.text
        mProfile.mNobind = mNobind.isChecked
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        if (preference === mUsePull || preference === mOverrideDNS)
            if (preference === mOverrideDNS) {
                // Set so the function gets the right value
                mOverrideDNS.isChecked = (newValue as Boolean)
            }
        setDNSState()
        saveSettings()
        return true
    }

    override fun onPause() {
        super.onPause()
        saveSettings()
    }

    private fun setDNSState() {
        val enabled: Boolean
        mOverrideDNS.isEnabled = mUsePull.isChecked
        enabled = if (!mUsePull.isChecked) true else mOverrideDNS.isChecked
        mDNS1.isEnabled = enabled
        mDNS2.isEnabled = enabled
        mSearchdomain.isEnabled = enabled
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

    }
}

class DNSSummaryProvider : Preference.SummaryProvider<Preference> {
    override fun provideSummary(preference: Preference): CharSequence {
        val ep = preference as EditTextPreference
        var summary = ep.text ?: ""
        if (summary == "8.8.4.4" || summary == "8.8.8.8" || summary == "2001:4860:4860::8888" || summary == "2001:4860:4860::8844" )
            summary += " (dns.google.com)"
        else if (summary.startsWith("2606:4700:4700::") || summary.startsWith("1.1.1.") || summary.startsWith("1.0.0."))
            summary += " (Cloudflare)"

        else if (summary.startsWith("9.9.9.") || summary.startsWith("2620:fe::"))
            summary += " (Quad9)"
        else if (summary.isEmpty())
            summary = "(not set)"

        return summary
    }

}
