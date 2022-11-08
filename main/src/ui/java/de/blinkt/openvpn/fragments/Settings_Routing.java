/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import de.blinkt.openvpn.R;


public class Settings_Routing extends OpenVpnPreferencesFragment implements Preference.OnPreferenceChangeListener {
	private EditTextPreference mCustomRoutes;
	private CheckBoxPreference mUseDefaultRoute;
	private EditTextPreference mCustomRoutesv6;
	private CheckBoxPreference mUseDefaultRoutev6;
	private CheckBoxPreference mRouteNoPull;
    private CheckBoxPreference mLocalVPNAccess;
    private EditTextPreference mExcludedRoutes;
    private EditTextPreference mExcludedRoutesv6;
	private CheckBoxPreference mBlockUnusedAF;
	private CheckBoxPreference mTorOverVpn;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_routing);
        mCustomRoutes = findPreference("customRoutes");
		mUseDefaultRoute = findPreference("useDefaultRoute");
		mCustomRoutesv6 = findPreference("customRoutesv6");
		mUseDefaultRoutev6 = findPreference("useDefaultRoutev6");
        mExcludedRoutes = findPreference("excludedRoutes");
        mExcludedRoutesv6 = findPreference("excludedRoutesv6");

		mRouteNoPull = findPreference("routenopull");
        mLocalVPNAccess = findPreference("unblockLocal");
		mTorOverVpn = findPreference("torOverVpn");

		mBlockUnusedAF = findPreference("blockUnusedAF");

		mCustomRoutes.setOnPreferenceChangeListener(this);
		mCustomRoutesv6.setOnPreferenceChangeListener(this);
        mExcludedRoutes.setOnPreferenceChangeListener(this);
        mExcludedRoutesv6.setOnPreferenceChangeListener(this);
		mBlockUnusedAF.setOnPreferenceChangeListener(this);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			getPreferenceScreen().removePreference(mBlockUnusedAF);

		loadSettings();
	}

	@Override
	public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

	}

	@Override
	protected void loadSettings() {

		mUseDefaultRoute.setChecked(mProfile.mUseDefaultRoute);
		mUseDefaultRoutev6.setChecked(mProfile.mUseDefaultRoutev6);

		mCustomRoutes.setText(mProfile.mCustomRoutes);
		mCustomRoutesv6.setText(mProfile.mCustomRoutesv6);

        mExcludedRoutes.setText(mProfile.mExcludedRoutes);
        mExcludedRoutesv6.setText(mProfile.mExcludedRoutesv6);

		mRouteNoPull.setChecked(mProfile.mRoutenopull);
		mTorOverVpn.setChecked(mProfile.mTorOverVpn);
        mLocalVPNAccess.setChecked(mProfile.mAllowLocalLAN);

        mBlockUnusedAF.setChecked(mProfile.mBlockUnusedAddressFamilies);

		// Sets Summary
		onPreferenceChange(mCustomRoutes, mCustomRoutes.getText());
		onPreferenceChange(mCustomRoutesv6, mCustomRoutesv6.getText());
        onPreferenceChange(mExcludedRoutes, mExcludedRoutes.getText());
        onPreferenceChange(mExcludedRoutesv6, mExcludedRoutesv6.getText());

	}


	@Override
	protected void saveSettings() {
		mProfile.mUseDefaultRoute = mUseDefaultRoute.isChecked();
		mProfile.mUseDefaultRoutev6 = mUseDefaultRoutev6.isChecked();
		mProfile.mCustomRoutes = mCustomRoutes.getText();
		mProfile.mCustomRoutesv6 = mCustomRoutesv6.getText();
		mProfile.mRoutenopull = mRouteNoPull.isChecked();
		mProfile.mTorOverVpn = mTorOverVpn.isChecked();
        mProfile.mAllowLocalLAN =mLocalVPNAccess.isChecked();
        mProfile.mExcludedRoutes = mExcludedRoutes.getText();
        mProfile.mExcludedRoutesv6 = mExcludedRoutesv6.getText();
        mProfile.mBlockUnusedAddressFamilies = mBlockUnusedAF.isChecked();
	}

	@Override
	public boolean onPreferenceChange(Preference preference,
			Object newValue) {
		if(	 preference == mCustomRoutes || preference == mCustomRoutesv6
                || preference == mExcludedRoutes || preference == mExcludedRoutesv6)
			preference.setSummary((String)newValue);

		saveSettings();
		return true;
	}


}