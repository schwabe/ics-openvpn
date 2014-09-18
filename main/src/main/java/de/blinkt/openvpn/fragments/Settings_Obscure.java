/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.widget.Toast;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

public class Settings_Obscure extends OpenVpnPreferencesFragment implements OnPreferenceChangeListener {
	private CheckBoxPreference mUseRandomHostName;
	private CheckBoxPreference mUseFloat;
	private CheckBoxPreference mUseCustomConfig;
	private EditTextPreference mCustomConfig;
    private EditTextPreference mMssFixValue;
    private CheckBoxPreference mMssFixCheckBox;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_obscure);
		
		mUseRandomHostName = (CheckBoxPreference) findPreference("useRandomHostname");
		mUseFloat = (CheckBoxPreference) findPreference("useFloat");
		mUseCustomConfig = (CheckBoxPreference) findPreference("enableCustomOptions");
		mCustomConfig = (EditTextPreference) findPreference("customOptions");
        mMssFixCheckBox = (CheckBoxPreference) findPreference("mssFix");
        mMssFixValue = (EditTextPreference) findPreference("mssFixValue");
        mMssFixValue.setOnPreferenceChangeListener(this);
		
		loadSettings();

	}

    protected void loadSettings() {
        mUseRandomHostName.setChecked(mProfile.mUseRandomHostname);
        mUseFloat.setChecked(mProfile.mUseFloat);
        mUseCustomConfig.setChecked(mProfile.mUseCustomConfig);
        mCustomConfig.setText(mProfile.mCustomConfigOptions);

        if (mProfile.mMssFix == 0) {
            mMssFixValue.setText(String.valueOf(VpnProfile.DEFAULT_MSSFIX_SIZE));
            mMssFixCheckBox.setChecked(false);
            setMssSummary(VpnProfile.DEFAULT_MSSFIX_SIZE);
        } else {
            mMssFixValue.setText(String.valueOf(mProfile.mMssFix));
            mMssFixCheckBox.setChecked(true);
            setMssSummary(mProfile.mMssFix);
        }

    }

    private void setMssSummary(int value) {
        mMssFixValue.setSummary(String.format("Configured MSS value: %d", value));
    }

    protected void saveSettings() {
		mProfile.mUseRandomHostname = mUseRandomHostName.isChecked();
		mProfile.mUseFloat = mUseFloat.isChecked();
		mProfile.mUseCustomConfig = mUseCustomConfig.isChecked();
		mProfile.mCustomConfigOptions = mCustomConfig.getText();
        if (mMssFixCheckBox.isChecked())
            mProfile.mMssFix=Integer.parseInt(mMssFixValue.getText());
        else
            mProfile.mMssFix=0;

	}

	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference.getKey().equals("mssFixValue"))
            try {
                int v = Integer.parseInt((String) newValue);
                if (v < 0 || v > 9000)
                    throw new NumberFormatException("mssfix value");
                setMssSummary(v);

            } catch(NumberFormatException e) {
                Toast.makeText(getActivity(), R.string.mssfix_invalid_value, Toast.LENGTH_LONG).show();
                return false;
            }
        return true;
	}

}