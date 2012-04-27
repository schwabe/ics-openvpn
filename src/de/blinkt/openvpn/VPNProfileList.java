package de.blinkt.openvpn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import de.blinkt.openvpn.VPNConfigPreference.VpnPreferencesClickListener;

public class VPNProfileList extends PreferenceFragment implements  VpnPreferencesClickListener {
	private static final int MENU_ADD_PROFILE = Menu.FIRST;

	private static final int START_VPN_PROFILECONFIG = 70;


	private VpnProfile mSelectedVPN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.vpn_profile_list);
		setHasOptionsMenu(true);

	}

	@Override
	public void onResume() {
		super.onResume();
		refreshList();


	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(0, MENU_ADD_PROFILE, 0, R.string.menu_add_profile)
		.setIcon(android.R.drawable.ic_menu_add)
		.setAlphabeticShortcut('a')
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == MENU_ADD_PROFILE) {
			onAddProfileClicked();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void onAddProfileClicked() {
		Context context = getActivity();
		if (context != null) {
			final EditText entry = new EditText(context);
			entry.setSingleLine();

			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setTitle(R.string.menu_add_profile);
			dialog.setMessage(R.string.add_profile_name_prompt);
			dialog.setView(entry);


			dialog.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = entry.getText().toString();
					if (getPM().getProfileByName(name)==null) {
						VpnProfile profile = new VpnProfile(name);
						addProfile(profile);
						refreshList();
					} else {
						Toast.makeText(getActivity(), R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
					}
				}


			});
			dialog.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.create().show();
		}

	}

	
	private void addProfile(VpnProfile profile) {
		getPM().addProfile(profile);
		getPM().saveProfileList(getActivity());
		saveProfile(profile);

	}


	private void saveProfile(VpnProfile profile) {
		ObjectOutputStream vpnfile;
		try {
			vpnfile = new ObjectOutputStream(getActivity().openFileOutput((profile.getUUID().toString() + ".vp"),Activity.MODE_PRIVATE));

			vpnfile.writeObject(profile);
			vpnfile.flush();
			vpnfile.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}

	

	public void refreshList() {
		PreferenceScreen plist = getPreferenceScreen();
		if (plist != null) {
			plist.removeAll(); 
			getPM().loadVPNList(getActivity());

			for (VpnProfile vpnprofile: getPM().getProfiles()) {
				Bundle args = new Bundle();
				//TODO

				String profileuuid = vpnprofile.getUUID().toString();


				args.putSerializable(getActivity().getPackageName() + ".VpnProfile", vpnprofile);
				//args.putString("name", vpnentry);
				VPNConfigPreference vpref = new VPNConfigPreference(this, args);
				vpref.setKey(profileuuid);
				vpref.setTitle(vpnprofile.getName());
				vpref.setPersistent(false);
				//				vpref.setSelectable(true);
				vpref.setOnQuickSettingsClickListener(this);
				plist.addPreference(vpref);
			}


		}
	}



	private ProfileManager getPM() {
		return ProfileManager.getInstance();
	}

	@Override
	public boolean onQuickSettingsClick(Preference preference) {
		String key = preference.getKey();

		VpnProfile vprofile = ProfileManager.get(key);

		Intent vprefintent = new Intent(getActivity(),VPNPreferences.class)
		.putExtra(getActivity().getPackageName() + ".VpnProfile", vprofile);

		startActivityForResult(vprefintent,START_VPN_PROFILECONFIG);
		return true;
	}





	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode==START_VPN_PROFILECONFIG) {
			new startOpenVpnThread().start();;

		}

	}

	private class startOpenVpnThread extends Thread {

		@Override
		public void run() {
			startOpenVpn();
		}

		void startOpenVpn() {
			Intent startVPN = mSelectedVPN.prepareIntent(getActivity());
		
			getActivity().startService(startVPN);
			Intent startLW = new Intent(getActivity().getBaseContext(),LogWindow.class);
			startActivity(startLW);
			getActivity().finish();
		}
	}
	
	@Override
	public void onStartVPNClick(VPNConfigPreference preference) {
		// Query the System for permission 
		mSelectedVPN = getPM().get(preference.getKey());
		Intent intent = VpnService.prepare(getActivity());

		if (intent != null) {
			// Start the query
			intent.putExtra("FOO", "WAR BIER");
			startActivityForResult(intent, 0);
		} else {
			onActivityResult(START_VPN_PROFILECONFIG, Activity.RESULT_OK, null);
		}

	}
}
