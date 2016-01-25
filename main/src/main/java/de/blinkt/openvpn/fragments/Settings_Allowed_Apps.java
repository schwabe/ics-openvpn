/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

/**
 * Created by arne on 16.11.14.
 */
public class Settings_Allowed_Apps extends Fragment implements AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener, View.OnClickListener {
    private ListView mListView;
    private VpnProfile mProfile;
    private TextView mDefaultAllowTextView;
    private PackageAdapter mListAdapter;

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        AppViewHolder avh = (AppViewHolder) view.getTag();
        avh.checkBox.toggle();
    }

    @Override
    public void onClick(View v) {

    }

    static class AppViewHolder {
        public ApplicationInfo mInfo;
        public View rootView;
        public TextView appName;
        public ImageView appIcon;
        //public TextView appSize;
        //public TextView disabled;
        public CompoundButton checkBox;

        static public AppViewHolder createOrRecycle(LayoutInflater inflater, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.allowed_application_layout, parent, false);

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


    class PackageAdapter extends BaseAdapter implements Filterable {
        private Vector<ApplicationInfo> mPackages;
        private final LayoutInflater mInflater;
        private final PackageManager mPm;
        private ItemFilter mFilter = new ItemFilter();
        private Vector<ApplicationInfo> mFilteredData;


        private class ItemFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String filterString = constraint.toString().toLowerCase(Locale.getDefault());

                FilterResults results = new FilterResults();


                int count = mPackages.size();
                final Vector<ApplicationInfo> nlist = new Vector<>(count);

                for (int i = 0; i < count; i++) {
                    ApplicationInfo pInfo = mPackages.get(i);
                    CharSequence appName = pInfo.loadLabel(mPm);

                    if (TextUtils.isEmpty(appName))
                        appName = pInfo.packageName;

                    if (appName instanceof  String) {
                        if (((String) appName).toLowerCase(Locale.getDefault()).contains(filterString))
                                nlist.add(pInfo);
                    } else {
                        if (appName.toString().toLowerCase(Locale.getDefault()).contains(filterString))
                            nlist.add(pInfo);
                    }
                }
                results.values = nlist;
                results.count = nlist.size();

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredData = (Vector<ApplicationInfo>) results.values;
                notifyDataSetChanged();
            }

        }


        PackageAdapter(Context c, VpnProfile vp) {
            mPm = c.getPackageManager();
            mProfile = vp;
            mInflater = LayoutInflater.from(c);

            mPackages = new Vector<>();
            mFilteredData = mPackages;
        }

        private void populateList(Activity c) {
            List<ApplicationInfo> installedPackages = mPm.getInstalledApplications(PackageManager.GET_META_DATA);

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
            mFilteredData = apps;
            c.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getCount() {
            return mFilteredData.size();
        }

        @Override
        public Object getItem(int position) {
            return mFilteredData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mFilteredData.get(position).packageName.hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AppViewHolder viewHolder = AppViewHolder.createOrRecycle(mInflater, convertView, parent);
            convertView = viewHolder.rootView;
            viewHolder.mInfo = mFilteredData.get(position);
            final ApplicationInfo mInfo = mFilteredData.get(position);


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

        @Override
        public Filter getFilter() {
            return mFilter;
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
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.allowed_apps, menu);

        SearchView searchView = (SearchView) menu.findItem( R.id.app_search_widget ).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mListView.setFilterText(query);
                mListView.setTextFilterEnabled(true);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mListView.setFilterText(newText);
                if (TextUtils.isEmpty(newText))
                    mListView.setTextFilterEnabled(false);
                else
                    mListView.setTextFilterEnabled(true);

                return true;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mListView.clearTextFilter();
                mListAdapter.getFilter().filter("");
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
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

        mListView.setEmptyView(v.findViewById(R.id.loading_container));

        new Thread(new Runnable() {
            @Override
            public void run() {
                mListAdapter.populateList(getActivity());
            }
        }).start();

        return v;
    }

    private void changeDisallowText(boolean selectedAreDisallowed) {
        if (selectedAreDisallowed)
            mDefaultAllowTextView.setText(R.string.vpn_disallow_radio);
        else
            mDefaultAllowTextView.setText(R.string.vpn_allow_radio);
    }
}
