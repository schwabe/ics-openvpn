package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;

public class Settings_Authentication extends PreferenceFragment implements OnPreferenceChangeListener {
	private CheckBoxPreference mExpectTLSCert;
	private CheckBoxPreference mCheckRemoteCN;
	private EditTextPreference mRemoteCN;
	private VpnProfile mProfile;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_authentification);

		mExpectTLSCert = (CheckBoxPreference) findPreference("remoteServerTLS");
		mCheckRemoteCN = (CheckBoxPreference) findPreference("checkRemoteCN");
		mRemoteCN = (EditTextPreference) findPreference("remotecn");
		mRemoteCN.setOnPreferenceChangeListener(this);
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile = ProfileManager.get(profileUUID);
				
		loadSettings();

	}

	private void loadSettings() {
		
		mExpectTLSCert.setChecked(mProfile.mExpectTLSCert);
		mCheckRemoteCN.setChecked(mProfile.mCheckRemoteCN);
		mRemoteCN.setText(mProfile.mRemoteCN);
		onPreferenceChange(mRemoteCN, mProfile.mRemoteCN);
		
	}
	
	private void saveSettings() {
		mProfile.mExpectTLSCert=mExpectTLSCert.isChecked();
		mProfile.mCheckRemoteCN=mCheckRemoteCN.isChecked();
		mProfile.mRemoteCN=mRemoteCN.getText();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		saveSettings();
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference==mRemoteCN) {
			if ("".equals(newValue))
				preference.setSummary(mProfile.mServerName);
			else
				preference.setSummary((String)newValue);
		}
		saveSettings();
		return true;
	}
}