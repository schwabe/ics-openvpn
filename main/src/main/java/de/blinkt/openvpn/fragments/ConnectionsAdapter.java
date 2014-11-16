/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Arrays;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;

/**
 * Created by arne on 30.10.14.
 */
public class ConnectionsAdapter extends BaseAdapter {
    private final Context mContext;
    private final VpnProfile mProfile;
    private final Settings_Connections mConnectionFragment;
    private Connection[] mConnections;

    public ConnectionsAdapter(Context c, Settings_Connections connections_fragments, VpnProfile vpnProfile)
    {
        mContext = c;
        mConnections = vpnProfile.mConnections;
        mProfile = vpnProfile;
        mConnectionFragment = connections_fragments;
    }

    @Override
    public int getCount() {
        return mConnections.length;
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    // create a new ImageView for each item referenced by the Adapter
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View card;
        if (convertView==null) {
            LayoutInflater li = LayoutInflater.from(mContext);
            card = li.inflate(R.layout.server_card, parent, false);
        } else {
            card = convertView;
        }
        final Connection connection = mConnections[position];
        ((TextView)card.findViewById(R.id.portnumber)).
                setText(connection.mServerPort);

        EditText serverNameView = (EditText) card.findViewById(R.id.servername);
        EditText portNumberView = (EditText) card.findViewById(R.id.portnumber);

        serverNameView.setText(connection.mServerName);
        portNumberView.setText(connection.mServerPort);

        Switch remoteSwitch = (Switch) card.findViewById (R.id.remoteSwitch);
        remoteSwitch.setChecked(connection.mEnabled);
        remoteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                connection.mEnabled = isChecked;
                displayWarningifNoneEnabled();
            }
        });


        serverNameView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                connection.mServerName = s.toString();
            }
        });

        portNumberView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                connection.mServerPort = s.toString();
            }
        });

        CheckBox customOptionCB = (CheckBox) card.findViewById(R.id.use_customoptions);
        final EditText editText = (EditText) card.findViewById(R.id.customoptions);
        RadioGroup protoGroup = (RadioGroup) card.findViewById(R.id.udptcpradiogroup);
        protoGroup.check(connection.mUseUdp ? R.id.udp_proto : R.id.tcp_proto);
        protoGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.udp_proto)
                    connection.mUseUdp=true;
                else if (checkedId == R.id.tcp_proto)
                    connection.mUseUdp=false;
            }
        });

        final View customOptionsLayout = card.findViewById(R.id.custom_options_layout);

        customOptionsLayout.setVisibility(connection.mUseCustomConfig ? View.VISIBLE : View.GONE);
        editText.setText(connection.mCustomConfiguration);

        customOptionCB.setChecked(connection.mUseCustomConfig);
        customOptionCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                connection.mUseCustomConfig = isChecked;
                customOptionsLayout.setVisibility(connection.mUseCustomConfig ? View.VISIBLE : View.GONE);
            }
        });
        displayWarningifNoneEnabled();
        return card;
    }

    private void displayWarningifNoneEnabled() {
        int showWarning = View.VISIBLE;
        for(Connection conn:mConnections) {
            if(conn.mEnabled)
                showWarning= View.GONE;
        }
        mConnectionFragment.setWarningVisible(showWarning);
    }

    public void addRemote() {
        mConnections = Arrays.copyOf(mConnections, mConnections.length+1);
        mConnections[mConnections.length-1] = new Connection();

        notifyDataSetInvalidated();
    }

    public void saveProfile() {
        mProfile.mConnections= mConnections;
    }
}
