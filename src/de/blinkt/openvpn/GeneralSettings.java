package de.blinkt.openvpn;
import java.io.File;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

public class GeneralSettings extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.general_settings);
			Preference loadtun = findPreference("loadTunModule");
			if(!isTunModuleAvailable())
				loadtun.setEnabled(false);
		}

		private boolean isTunModuleAvailable() {
			// Check if the tun module exists on the file system
			if(new File("/system/lib/modules/tun.ko").length() > 10)
				return true;
			return false;
		}

	
	}