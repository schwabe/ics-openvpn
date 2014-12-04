/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

/**
 * Created by arne on 16.11.14.
 */
public class Settings_Allowed_Apps extends Fragment implements AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {
    private ListView mListView;
    private VpnProfile mProfile;
    private TextView mDefaultAllowTextView;
    private PackageAdapter mListAdapter;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppViewHolder avh = (AppViewHolder) view.getTag();
        avh.checkBox.toggle();
    }

    static class AppViewHolder {
        public ApplicationInfo mInfo;
        public View rootView;
        public TextView appName;
        public ImageView appIcon;
        //public TextView appSize;
        //public TextView disabled;
        public CompoundButton checkBox;

        static public AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.allowed_application_layout, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                AppViewHolder holder = new AppViewHolder();
                holder.rootView = convertView;
                holder.appName = (TextView) convertView.findViewById(R.id.app_name);
                holder.appIcon = (ImageView) convertView.findViewById(R.id.app_icon);
                //holder.appSize = (TextView) convertView.findViewById(R.id.app_size);
                //holder.disabled = (TextView) convertView.findViewById(R.id.app_disabled);
                holder.checkBox = (CompoundButton) convertView.findViewById(R.id.app_selected);
                convertView.setTag(holder);


                return holder;
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                return (AppViewHolder) convertView.getTag();
            }
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        String packageName = (String) buttonView.getTag();
        if (isChecked) {
            Log.d("openvpn", "adding to allowed apps" + packageName);
            mProfile.mAllowedAppsVpn.add(packageName);
        } else {
            Log.d("openvpn", "removing from allowed apps" + packageName);
            mProfile.mAllowedAppsVpn.remove(packageName);
        }

    }


    class PackageAdapter extends BaseAdapter {
        private final List<ApplicationInfo> mPackages;
        private final LayoutInflater mInflater;
        private final PackageManager mPm;

        PackageAdapter(Context c, VpnProfile vp) {
            mPm = c.getPackageManager();
            mProfile = vp;
            List<ApplicationInfo> installedPackages = mPm.getInstalledApplications(PackageManager.GET_META_DATA);
            mInflater = LayoutInflater.from(c);

            // Remove apps not using Internet

            int androidSystemUid = 0;
            ApplicationInfo system = null;
            Vector<ApplicationInfo> apps = new Vector<ApplicationInfo>();

            try {
                system = mPm.getApplicationInfo("android", PackageManager.GET_META_DATA);
                androidSystemUid = system.uid;
                apps.add(system);
            } catch (PackageManager.NameNotFoundException e) {
            }


            for (ApplicationInfo app : installedPackages) {

                if (mPm.checkPermission(Manifest.permission.INTERNET, app.packageName) == PackageManager.PERMISSION_GRANTED &&
                        app.uid != androidSystemUid) {

                    apps.add(app);
                }
            }

            Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(mPm));
            mPackages = apps;
        }

        @Override
        public int getCount() {
            return mPackages.size();
        }

        @Override
        public Object getItem(int position) {
            return mPackages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mPackages.get(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder viewHolder = AppViewHolder.createOrRecycle(mInflater, convertView);
            convertView = viewHolder.rootView;
            viewHolder.mInfo = mPackages.get(position);
            final ApplicationInfo mInfo = mPackages.get(position);


            CharSequence appName = mInfo.loadLabel(mPm);

            if (TextUtils.isEmpty(appName))
                appName = mInfo.packageName;
            viewHolder.appName.setText(appName);
            viewHolder.appIcon.setImageDrawable(mInfo.loadIcon(mPm));
            viewHolder.checkBox.setTag(mInfo.packageName);
            viewHolder.checkBox.setOnCheckedChangeListener(Settings_Allowed_Apps.this);


            viewHolder.checkBox.setChecked(mProfile.mAllowedAppsVpn.contains(mInfo.packageName));
            return viewHolder.rootView;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeDisallowText(mProfile.mAllowedAppsVpnAreDisallowed);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String profileUuid = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
        mProfile = ProfileManager.get(getActivity(), profileUuid);
        getActivity().setTitle(getString(R.string.edit_profile_title, mProfile.getName()));

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.allowed_vpn_apps, container, false);

        mDefaultAllowTextView = (TextView) v.findViewById(R.id.default_allow_text);

        Switch vpnOnDefaultSwitch = (Switch) v.findViewById(R.id.default_allow);

        vpnOnDefaultSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                changeDisallowText(isChecked);
                mProfile.mAllowedAppsVpnAreDisallowed = isChecked;
            }
        });

        vpnOnDefaultSwitch.setChecked(mProfile.mAllowedAppsVpnAreDisallowed);

        mListView = (ListView) v.findViewById(android.R.id.list);

        mListAdapter = new PackageAdapter(getActivity(), mProfile);
        mListView.setAdapter(mListAdapter);
        mListView.setOnItemClickListener(this);


        return v;
    }

    private void changeDisallowText(boolean selectedAreDisallowed) {
        if (selectedAreDisallowed)
            mDefaultAllowTextView.setText(R.string.vpn_disallow_radio);
        else
            mDefaultAllowTextView.setText(R.string.vpn_allow_radio);
    }
}
