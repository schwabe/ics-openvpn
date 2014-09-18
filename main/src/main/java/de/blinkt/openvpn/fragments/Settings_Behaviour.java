/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import de.blinkt.openvpn.R;

public class Settings_Behaviour extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener {
	private CheckBoxPreference mPersistent;
	private ListPreference mConnectretrymax;
	private EditTextPreference mConnectretry;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_behaviour);
		
		mPersistent = (CheckBoxPreference) findPreference("usePersistTun");
		mConnectretrymax = (ListPreference) findPreference("connectretrymax");
		mConnectretry = (EditTextPreference) findPreference("connectretry");
		
		mConnectretrymax.setOnPreferenceChangeListener(this);
		mConnectretrymax.setSummary("%s");
		
		mConnectretry.setOnPreferenceChangeListener(this);
		
		
		loadSettings();

	}
	
	protected void loadSettings() {
		mPersistent.setChecked(mProfile.mPersistTun);
		
		mConnectretrymax.setValue(mProfile.mConnectRetryMax);
		onPreferenceChange(mConnectretrymax, mProfile.mConnectRetryMax);
				
		mConnectretry.setText(mProfile.mConnectRetry);
		onPreferenceChange(mConnectretry, mProfile.mConnectRetry);
	}


	protected void saveSettings() {
		mProfile.mConnectRetryMax = mConnectretrymax.getValue();
		mProfile.mPersistTun = mPersistent.isChecked();
		mProfile.mConnectRetry = mConnectretry.getText();
	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
	     if (preference == mConnectretrymax) {
			if(newValue==null) {
				newValue="5";
			}
			mConnectretrymax.setDefaultValue(newValue);
			
			for(int i=0;i<mConnectretrymax.getEntryValues().length;i++){
				if(mConnectretrymax.getEntryValues().equals(newValue))
					mConnectretrymax.setSummary(mConnectretrymax.getEntries()[i]);
			}
			
		} else if (preference == mConnectretry) {
			if(newValue==null || newValue=="")
				newValue="5";
			mConnectretry.setSummary(String.format("%s s" , newValue));
		}
			
		return true;
	}

}