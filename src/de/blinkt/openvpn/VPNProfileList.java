package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.PreferenceActivity;


import android.app.ProfileManager;

public class VPNProfileList extends PreferenceActivity {
    private ProfileManager mProfileManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {
            addPreferencesFromResource(R.xml.profiles_settings);
            mProfileManager = (ProfileManager) getActivity().getSystemService(PROFILE_SERVICE);

        }
    }
}
