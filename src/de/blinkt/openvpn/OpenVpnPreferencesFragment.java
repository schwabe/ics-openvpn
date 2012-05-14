package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public abstract class OpenVpnPreferencesFragment extends PreferenceFragment {
	
	protected VpnProfile mProfile;

	protected abstract void loadSettings();
	protected abstract void saveSettings();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Make sure there is an instance of the profile manager
		ProfileManager.getInstance(getActivity());
		
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile = ProfileManager.get(profileUUID);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		saveSettings();
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState!=null) {
			String profileUUID=savedInstanceState.getString(VpnProfile.EXTRA_PROFILEUUID);
			ProfileManager.getInstance(getActivity());
			mProfile = ProfileManager.get(profileUUID);
			loadSettings();
		}
	}
	
	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}
	
	@Override
	public void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		saveSettings();
		outState.putString(VpnProfile.EXTRA_PROFILEUUID, mProfile.getUUIDString());
	}
}
