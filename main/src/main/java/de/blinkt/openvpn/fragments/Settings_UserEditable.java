/*
 * Copyright (c) 2012-2015 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.blinkt.openvpn.R;

public class Settings_UserEditable extends OpenVpnPreferencesFragment {
    @Override
    protected void loadSettings() {

    }

    @Override
    protected void saveSettings() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_usereditable, container, false);
        TextView messageView = (TextView) v.findViewById(R.id.messageUserEdit);
        messageView.setText(getString(R.string.message_no_user_edit, getPackageString(mProfile.mProfileCreator)));
        return v;
    }


    private String getPackageString(String packageName) {
        final PackageManager pm = getActivity().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo( packageName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");
        return String.format("%s (%s)", applicationName, packageName);
    }
}
