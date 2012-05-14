package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

public class Settings_Obscure extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener {
	private CheckBoxPreference mUseRandomHostName;
	private CheckBoxPreference mUseFloat;
	private CheckBoxPreference mUseCustomConfig;
	private EditTextPreference mCustomConfig;
	private ListPreference mLogverbosity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_obscure);
		
		
		mUseRandomHostName = (CheckBoxPreference) findPreference("useRandomHostname");
		mUseFloat = (CheckBoxPreference) findPreference("useFloat");
		mUseCustomConfig = (CheckBoxPreference) findPreference("enableCustomOptions");
		mCustomConfig = (EditTextPreference) findPreference("customOptions");
		mLogverbosity = (ListPreference) findPreference("verblevel");
		
		mLogverbosity.setOnPreferenceChangeListener(this);
		mLogverbosity.setSummary("%s");
		
		loadSettings();

	}
	
	protected void loadSettings() {
		mUseRandomHostName.setChecked(mProfile.mUseRandomHostname);
		mUseFloat.setChecked(mProfile.mUseFloat);
		mUseCustomConfig.setChecked(mProfile.mUseCustomConfig);
		mCustomConfig.setText(mProfile.mCustomConfigOptions);
		
		mLogverbosity.setValue(mProfile.mVerb);
		onPreferenceChange(mLogverbosity, mProfile.mVerb);
	}


	protected void saveSettings() {
		mProfile.mUseRandomHostname = mUseRandomHostName.isChecked();
		mProfile.mUseFloat = mUseFloat.isChecked();
		mProfile.mUseCustomConfig = mUseCustomConfig.isChecked();
		mProfile.mCustomConfigOptions = mCustomConfig.getText();
		mProfile.mVerb = mLogverbosity.getValue();
	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if(preference==mLogverbosity) {
			// Catch old version problem
			if(newValue==null){
				newValue="1";
			}
			mLogverbosity.setDefaultValue(newValue);
			//This is idiotic. 
			int i =Integer.parseInt((String) newValue);
			mLogverbosity.setSummary(mLogverbosity.getEntries()[i]);
		}
			
		return true;
	}

}