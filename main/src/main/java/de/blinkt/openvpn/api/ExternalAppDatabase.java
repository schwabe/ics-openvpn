/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.api;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.VpnStatus;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class ExternalAppDatabase {

	Context mContext;
	
	public ExternalAppDatabase(Context c) {
		mContext =c;
	}

	private final static String PREFERENCES_KEY = "allowed_apps";
	private final static String PREFERENCES_KEY_MANAGED_CONFIG = "allowed_apps_managed";

	public void setFlagManagedConfiguration(boolean managed)
	{
		SharedPreferences prefs = Preferences.getDefaultSharedPreferences(mContext);
		Editor prefedit = prefs.edit();

		prefedit.putBoolean(PREFERENCES_KEY_MANAGED_CONFIG, managed);
		increaseWorkaroundCounter(prefs, prefedit);
		prefedit.apply();
	}

	public boolean isManagedConfiguration()
	{
		SharedPreferences prefs = Preferences.getDefaultSharedPreferences(mContext);
		return prefs.getBoolean(PREFERENCES_KEY_MANAGED_CONFIG, false);
	}

	private static void increaseWorkaroundCounter(SharedPreferences prefs, Editor prefedit) {
		// Workaround for bug
		int counter = prefs.getInt("counter", 0);
		prefedit.putInt("counter", counter + 1);
	}

	boolean isAllowed(String packagename) {
		Set<String> allowedapps = getExtAppList();

		return allowedapps.contains(packagename);
	}

	public Set<String> getExtAppList() {
		SharedPreferences prefs = Preferences.getDefaultSharedPreferences(mContext);
        return prefs.getStringSet(PREFERENCES_KEY, new HashSet<String>());
	}
	
	public void addApp(String packagename)
	{
		Set<String> allowedapps = getExtAppList();
		allowedapps.add(packagename);
		saveExtAppList(allowedapps);
	}

	public boolean checkAllowingModifyingRemoteControl(Context c) {
		if (isManagedConfiguration()) {
			Toast.makeText(c, "Remote control apps are manged by managed configuration, cannot change", Toast.LENGTH_LONG).show();
			VpnStatus.logError("Remote control apps are manged by managed configuration, cannot change");
			return false;
		}
		return true;
	}

	private void saveExtAppList( Set<String> allowedapps) {
		SharedPreferences prefs = Preferences.getDefaultSharedPreferences(mContext);
		Editor prefedit = prefs.edit();

		// Workaround for bug
		prefedit.putStringSet(PREFERENCES_KEY, allowedapps);
		increaseWorkaroundCounter(prefs, prefedit);
		prefedit.apply();
	}
	
	public void clearAllApiApps() {
		saveExtAppList(new HashSet<String>());
	}

	public void removeApp(String packagename) {
		Set<String> allowedapps = getExtAppList();
		allowedapps.remove(packagename);
		saveExtAppList(allowedapps);		
	}


	public String checkOpenVPNPermission(PackageManager pm) throws SecurityRemoteException {

		for (String appPackage : getExtAppList()) {
			ApplicationInfo app;
			try {
				app = pm.getApplicationInfo(appPackage, 0);
				if (Binder.getCallingUid() == app.uid) {
					return appPackage;
				}
			} catch (PackageManager.NameNotFoundException e) {
				// App not found. Remove it from the list
				if (!isManagedConfiguration())
					removeApp(appPackage);
			}
		}
		throw new SecurityException("Unauthorized OpenVPN API Caller");
	}


	public boolean checkRemoteActionPermission(Context c, String callingPackage) {
		if (callingPackage == null)
			callingPackage = ConfirmDialog.ANONYMOUS_PACKAGE;

		if (isAllowed(callingPackage)) {
			return true;
		} else {
			Intent confirmDialog = new Intent(c, ConfirmDialog.class);
			confirmDialog.addFlags(FLAG_ACTIVITY_NEW_TASK);
			confirmDialog.putExtra(ConfirmDialog.EXTRA_PACKAGE_NAME, callingPackage);
			c.startActivity(confirmDialog);
			return false;
		}
	}

	public void setAllowedApps(Set<String> restrictionApps) {
		saveExtAppList(restrictionApps);
	}
}
