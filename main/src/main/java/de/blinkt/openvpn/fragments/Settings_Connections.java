/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

public class Settings_Connections extends Fragment implements View.OnClickListener {
    private VpnProfile mProfile;
    private ConnectionsAdapter mConnectionsAdapter;
    private TextView mWarning;
    private CheckBox mUseRandomRemote;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String profileUuid = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
        mProfile= ProfileManager.get(getActivity(), profileUuid);
        getActivity().setTitle(getString(R.string.edit_profile_title, mProfile.getName()));

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            inflater.inflate(R.menu.connections, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.connections, container, false);

        GridView gridview = (GridView) v.findViewById(R.id.gridview);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Toast.makeText(getActivity(), "" + position, Toast.LENGTH_SHORT).show();
            }
        });

        mConnectionsAdapter = new ConnectionsAdapter(getActivity(), this, mProfile);
        gridview.setAdapter(mConnectionsAdapter);

        ImageButton fab_button = (ImageButton) v.findViewById(R.id.add_new_remote);
        if(fab_button!=null)
                fab_button.setOnClickListener(this);

        mUseRandomRemote = (CheckBox) v.findViewById(R.id.remote_random);
        mUseRandomRemote.setChecked(mProfile.mRemoteRandom);

        mWarning = (TextView) v.findViewById(R.id.noserver_active_warning);
        return v;
    }



    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_new_remote) {
            mConnectionsAdapter.addRemote();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.add_new_remote)
            mConnectionsAdapter.addRemote();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        mConnectionsAdapter.saveProfile();
        mProfile.mRemoteRandom = mUseRandomRemote.isChecked();
    }

    public void setWarningVisible(int showWarning) {
        mWarning.setVisibility(showWarning);
    }
}
