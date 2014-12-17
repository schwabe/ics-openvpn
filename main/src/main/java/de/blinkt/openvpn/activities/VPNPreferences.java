/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import java.util.List;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.view.Menu;
import android.view.MenuItem;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.fragments.Settings_Authentication;
import de.blinkt.openvpn.fragments.Settings_Basic;
import de.blinkt.openvpn.fragments.Settings_Behaviour;
import de.blinkt.openvpn.fragments.Settings_IP;
import de.blinkt.openvpn.fragments.Settings_Obscure;
import de.blinkt.openvpn.fragments.Settings_Routing;
import de.blinkt.openvpn.fragments.ShowConfigFragment;
import de.blinkt.openvpn.fragments.VPNProfileList;


public class VPNPreferences extends PreferenceActivity {

    static final Class validFragments[] = new Class[] {
        Settings_Authentication.class, Settings_Basic.class, Settings_IP.class,
            Settings_Obscure.class, Settings_Routing.class, ShowConfigFragment.class,
            Settings_Behaviour.class
    };

    private String mProfileUUID;
	private VpnProfile mProfile;

	public VPNPreferences() {
		super();
	}


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected boolean isValidFragment(String fragmentName) {
        for (Class c: validFragments)
            if (c.getName().equals(fragmentName))
                return true;
        return false;

    }

    @Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(getIntent().getStringExtra(getPackageName() + ".profileUUID"),mProfileUUID);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		

		if(intent!=null) {
			String profileUUID = intent.getStringExtra(getPackageName() + ".profileUUID");
			if(profileUUID==null) {
				Bundle initialArguments = getIntent().getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
				profileUUID =  initialArguments.getString(getPackageName() + ".profileUUID");
			}
			if(profileUUID!=null){

				mProfileUUID = profileUUID;
				mProfile = ProfileManager.get(this,mProfileUUID);

			}
		}
		// When a profile is deleted from a category fragment in hadset mod we need to finish
		// this activity as well when returning
		if (mProfile==null || mProfile.profileDleted) {
			setResult(VPNProfileList.RESULT_VPN_DELETED);
			finish();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mProfileUUID = getIntent().getStringExtra(getPackageName() + ".profileUUID");
		if(savedInstanceState!=null){
			String savedUUID = savedInstanceState.getString(getPackageName() + ".profileUUID");
			if(savedUUID!=null)
				mProfileUUID=savedUUID;
		}

		mProfile = ProfileManager.get(this,mProfileUUID);
		if(mProfile!=null) {
			setTitle(getString(R.string.edit_profile_title, mProfile.getName()));
		}
		super.onCreate(savedInstanceState);
	}



	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.vpn_headers, target); 
		for (Header header : target) {
			if(header.fragmentArguments==null)
				header.fragmentArguments = new Bundle();
			header.fragmentArguments.putString(getPackageName() + ".profileUUID",mProfileUUID);
		}
	}

	@Override
	public void onBackPressed() {
		setResult(RESULT_OK, getIntent());
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == R.id.remove_vpn)
			askProfileRemoval();
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.vpnpreferences_menu, menu);

		return super.onCreateOptionsMenu(menu);
	}

	private void askProfileRemoval() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle("Confirm deletion");
		dialog.setMessage(getString(R.string.remove_vpn_query, mProfile.mName));

		dialog.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeProfile(mProfile);
			}

		});
		dialog.setNegativeButton(android.R.string.no,null);
		dialog.create().show();
	}
	
	protected void removeProfile(VpnProfile profile) {
		ProfileManager.getInstance(this).removeProfile(this,profile);
		setResult(VPNProfileList.RESULT_VPN_DELETED);
		finish();
		
	}
}

