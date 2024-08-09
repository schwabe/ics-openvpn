/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

public abstract class OpenVpnPreferencesFragment extends PreferenceFragmentCompat {

	protected VpnProfile mProfile;

	protected abstract void loadSettings();
	protected abstract void saveSettings();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile = ProfileManager.get(getActivity(), profileUUID);
		getActivity().setTitle(getString(R.string.edit_profile_title, mProfile.getName()));

	}

	@Override
	public void onPause() {
		super.onPause();
		saveSettings();
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		if (savedInstanceState != null) {
			String profileUUID = savedInstanceState.getString(VpnProfile.EXTRA_PROFILEUUID);
			mProfile = ProfileManager.get(getActivity(), profileUUID);
			loadSettings();
		}
	}

    @Override
	public void onSaveInstanceState (Bundle outState) {
		super.onSaveInstanceState(outState);
		if (getView() != null) {
			//if we have no view, there is no point in trying to save anything.
			saveSettings();
		}
		outState.putString(VpnProfile.EXTRA_PROFILEUUID, mProfile.getUUIDString());
	}
}
