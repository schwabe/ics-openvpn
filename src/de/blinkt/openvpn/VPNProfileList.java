package de.blinkt.openvpn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import de.blinkt.openvpn.VPNConfigPreference.OnQuickSettingsClickListener;

public class VPNProfileList extends PreferenceFragment implements OnPreferenceClickListener, OnQuickSettingsClickListener {
	private static final String PREFS_NAME =  "VPNList";

	private static final int MENU_ADD_PROFILE = Menu.FIRST;


	private HashMap<String,VpnProfile> profiles;

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
					if (getProfileByName(name)==null) {
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

	public VpnProfile getProfileByName(String name) {
		for (VpnProfile vpnp : profiles.values()) {
			if(vpnp.getName().equals(name)) {
				return vpnp;
			}
		}
		return null;			
	}

	private void addProfile(VpnProfile profile) {
		profiles.put(profile.getUUID().toString(),profile);
		Editor editor = getActivity().getSharedPreferences(PREFS_NAME,Activity.MODE_PRIVATE).edit();
		editor.putStringSet("vpnlist", profiles.keySet());
		editor.commit();
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

	private void loadVPNList() {
		profiles = new HashMap<String, VpnProfile>();
		SharedPreferences settings =getActivity().getSharedPreferences(PREFS_NAME,Activity.MODE_PRIVATE);
		Set<String> vlist = settings.getStringSet("vpnlist", null);
		if(vlist==null){
			vlist = new HashSet<String>();
		}

		for (String vpnentry : vlist) {
			try {
				ObjectInputStream vpnfile = new ObjectInputStream(getActivity().openFileInput(vpnentry + ".vp"));
				VpnProfile vp = ((VpnProfile) vpnfile.readObject());

				profiles.put(vp.getUUID().toString(), vp);

			} catch (StreamCorruptedException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) { 
				e.printStackTrace();
			}
		}
	}


	public void refreshList() {
		PreferenceScreen plist = getPreferenceScreen();
		if (plist != null) {
			plist.removeAll(); 
			loadVPNList();

			for (VpnProfile vpnprofile: profiles.values()) {
				Bundle args = new Bundle();
				//TODO

				String profileuuid = vpnprofile.getUUID().toString();


				args.putParcelable("Profile", vpnprofile);
				//args.putString("name", vpnentry);
				VPNConfigPreference vpref = new VPNConfigPreference(this, args);
				vpref.setKey(profileuuid);
				vpref.setTitle(vpnprofile.getName());
				vpref.setPersistent(false);
				//				vpref.setSelectable(true);
				vpref.setOnPreferenceClickListener(this);
				vpref.setOnQuickSettingsClickListener(this);
				plist.addPreference(vpref);
			}

		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key= preference.getKey();


		return true;

	}

	@Override
	public boolean onQuickSettingsClick(Preference preference) {
		String key = preference.getKey();

		VpnProfile vprofile = profiles.get(key);

		Intent vprefintent = new Intent(getActivity(),VPNPreferences.class)
		.putExtra("VpnProfile", (Parcelable)vprofile);

		startActivity(vprefintent);
		return true;
	}
}
