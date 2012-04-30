package de.blinkt.openvpn;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

public class Settings_Authentication extends PreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener {
	private static final int SELECT_TLS_FILE = 23223232;
	private CheckBoxPreference mExpectTLSCert;
	private CheckBoxPreference mCheckRemoteCN;
	private EditTextPreference mRemoteCN;
	private VpnProfile mProfile;
	private ListPreference mTLSAuthDirection;
	private Preference mTLSAuthFile;
	private SwitchPreference mUseTLSAuth;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.vpn_authentification);

		mExpectTLSCert = (CheckBoxPreference) findPreference("remoteServerTLS");
		mCheckRemoteCN = (CheckBoxPreference) findPreference("checkRemoteCN");
		mRemoteCN = (EditTextPreference) findPreference("remotecn");
		mRemoteCN.setOnPreferenceChangeListener(this);
		
		mUseTLSAuth = (SwitchPreference) findPreference("useTLSAuth" );
		mTLSAuthFile = findPreference("tlsAuthFile");
		mTLSAuthDirection = (ListPreference) findPreference("tls_direction");
		
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile = ProfileManager.get(profileUUID);
		mTLSAuthFile.setOnPreferenceClickListener(this);		
		
		loadSettings();

	}

	private void loadSettings() {
		
		mExpectTLSCert.setChecked(mProfile.mExpectTLSCert);
		mCheckRemoteCN.setChecked(mProfile.mCheckRemoteCN);
		mRemoteCN.setText(mProfile.mRemoteCN);
		onPreferenceChange(mRemoteCN, mProfile.mRemoteCN);
		
		mUseTLSAuth.setChecked(mProfile.mUseTLSAuth);
		mTLSAuthFile.setSummary(mProfile.mTLSAuthFilename);
		mTLSAuthDirection.setValue(mProfile.mTLSAuthDirection);
	}
	
	private void saveSettings() {
		mProfile.mExpectTLSCert=mExpectTLSCert.isChecked();
		mProfile.mCheckRemoteCN=mCheckRemoteCN.isChecked();
		mProfile.mRemoteCN=mRemoteCN.getText();
		
		mProfile.mUseTLSAuth = mUseTLSAuth.isChecked();
		if(mTLSAuthFile.getSummary()==null)
			mProfile.mTLSAuthFilename=null;
		else
			mProfile.mTLSAuthFilename = mTLSAuthFile.getSummary().toString();
		
		if(mTLSAuthDirection.getValue()==null)
			mProfile.mTLSAuthDirection=null;
		else
			mProfile.mTLSAuthDirection = mTLSAuthDirection.getValue().toString();
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
		return true;
	}
	void startFileDialog() {
		Intent startFC = new Intent(getActivity(),FileDialog.class);
		startFC.putExtra(FileDialog.START_PATH, "/sdcard");
		startFC.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
	
		startActivityForResult(startFC,SELECT_TLS_FILE);
	}
	@Override
	public boolean onPreferenceClick(Preference preference) {
		startFileDialog();
		return true;
		
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==SELECT_TLS_FILE && resultCode == Activity.RESULT_OK){
			   String filepath = data.getStringExtra(FileDialog.RESULT_PATH);
			   mTLSAuthFile.setSummary(filepath);
			
		}
	}
}