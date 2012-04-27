package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class Settings_Obscure extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Make sure default values are applied.  In a real app, you would
		// want this in a shared function that is used to retrieve the
		// SharedPreferences wherever they are needed.
		PreferenceManager.setDefaultValues(getActivity(),
				R.xml.vpn_ipsettings, false);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_obscure);
	}
}