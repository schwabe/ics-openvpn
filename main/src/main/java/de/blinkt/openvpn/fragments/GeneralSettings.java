/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;
import java.io.File;
import java.util.Collection;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import de.blinkt.openvpn.BuildConfig;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.api.ExternalAppDatabase;
import de.blinkt.openvpn.core.ProfileManager;


public class GeneralSettings extends PreferenceFragment implements OnPreferenceClickListener, OnClickListener, Preference.OnPreferenceChangeListener {

	private ExternalAppDatabase mExtapp;
	private ListPreference mAlwaysOnVPN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.general_settings);


        PreferenceCategory devHacks = (PreferenceCategory) findPreference("device_hacks");
		mAlwaysOnVPN = (ListPreference) findPreference("alwaysOnVpn");
        mAlwaysOnVPN.setOnPreferenceChangeListener(this);


        Preference loadtun = findPreference("loadTunModule");
		if(!isTunModuleAvailable()) {
			loadtun.setEnabled(false);
            devHacks.removePreference(loadtun);
        }

        CheckBoxPreference cm9hack = (CheckBoxPreference) findPreference("useCM9Fix");
        if (!cm9hack.isChecked() && (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1)) {
            devHacks.removePreference(cm9hack);
        }

		mExtapp = new ExternalAppDatabase(getActivity());
		Preference clearapi = findPreference("clearapi");
		clearapi.setOnPreferenceClickListener(this);


        if(devHacks.getPreferenceCount()==0)
            getPreferenceScreen().removePreference(devHacks);

        if (!"ovpn3".equals(BuildConfig.FLAVOR)) {
            PreferenceCategory appBehaviour = (PreferenceCategory) findPreference("app_behaviour");
            appBehaviour.removePreference(findPreference("ovpn3"));
        }


		setClearApiSummary();
	}

	@Override
	public void onResume() {
		super.onResume();

        ProfileManager pm = ProfileManager.getInstance(getActivity());
        Collection<VpnProfile> profiles = pm.getProfiles();
        CharSequence[] entries = new CharSequence[profiles.size()];
        CharSequence[] entryValues = new CharSequence[profiles.size()];;

        int i=0;
        for (VpnProfile p: profiles)
        {
            entries[i]=p.getName();
            entryValues[i]=p.getUUIDString();
            i++;
        }

        mAlwaysOnVPN.setEntries(entries);
        mAlwaysOnVPN.setEntryValues(entryValues);


        VpnProfile vpn = ProfileManager.getAlwaysOnVPN(getActivity());
		StringBuffer sb = new StringBuffer(getString(R.string.defaultvpnsummary));
		sb.append('\n');
        if (vpn== null)
            sb.append(getString(R.string.novpn_selected));
        else
           sb.append(getString(R.string.vpnselected, vpn.getName()));
		mAlwaysOnVPN.setSummary(sb.toString());

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference== mAlwaysOnVPN) {
            VpnProfile vpn = ProfileManager.get(getActivity(), (String) newValue);
            mAlwaysOnVPN.setSummary(vpn.getName());
        }
        return true;
    }

    private void setClearApiSummary() {
		Preference clearapi = findPreference("clearapi");

		if(mExtapp.getExtAppList().isEmpty()) {
			clearapi.setEnabled(false);
			clearapi.setSummary(R.string.no_external_app_allowed);
		} else { 
			clearapi.setEnabled(true);
			clearapi.setSummary(getString(R.string.allowed_apps,getExtAppList(", ")));
		}
	}

	private String getExtAppList(String delim) {
		ApplicationInfo app;
		PackageManager pm = getActivity().getPackageManager();

		String applist=null;
		for (String packagename : mExtapp.getExtAppList()) {
			try {
				app = pm.getApplicationInfo(packagename, 0);
				if (applist==null)
					applist = "";
				else
					applist += delim;
				applist+=app.loadLabel(pm);

			} catch (NameNotFoundException e) {
				// App not found. Remove it from the list
				mExtapp.removeApp(packagename);
			}
		}

		return applist;
	}

	private boolean isTunModuleAvailable() {
		// Check if the tun module exists on the file system
        return new File("/system/lib/modules/tun.ko").length() > 10;
    }

	@Override
	public boolean onPreferenceClick(Preference preference) { 
		if(preference.getKey().equals("clearapi")){
			Builder builder = new AlertDialog.Builder(getActivity());
			builder.setPositiveButton(R.string.clear, this);
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.setMessage(getString(R.string.clearappsdialog,getExtAppList("\n")));
			builder.show();
		}
			
		return true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if( which == Dialog.BUTTON_POSITIVE){
			mExtapp.clearAllApiApps();
			setClearApiSummary();
		}
	}



}