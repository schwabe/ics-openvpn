/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;

import java.util.Arrays;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;

/**
 * Created by arne on 30.10.14.
 */
public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ConnectionsHolder>  {
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

    public static class ConnectionsHolder extends RecyclerView.ViewHolder {
        private final EditText mServerNameView;
        private final EditText mPortNumberView;
        private final Switch mRemoteSwitch;
        private final RadioGroup mProtoGroup;
        private final EditText mCustomOptionText;
        private final CheckBox mCustomOptionCB;
        private final View mCustomOptionsLayout;
        private final ImageButton mDeleteButton;

        public ConnectionsHolder(View card) {
            super(card);
            mServerNameView = (EditText) card.findViewById(R.id.servername);
            mPortNumberView = (EditText) card.findViewById(R.id.portnumber);
            mRemoteSwitch = (Switch) card.findViewById (R.id.remoteSwitch);
            mCustomOptionCB = (CheckBox) card.findViewById(R.id.use_customoptions);
            mCustomOptionText = (EditText) card.findViewById(R.id.customoptions);
            mProtoGroup = (RadioGroup) card.findViewById(R.id.udptcpradiogroup);
            mCustomOptionsLayout = card.findViewById(R.id.custom_options_layout);
            mDeleteButton = (ImageButton) card.findViewById(R.id.remove_connection);

        }
    }

    @Override
    public ConnectionsAdapter.ConnectionsHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        LayoutInflater li = LayoutInflater.from(mContext);
        View card = li.inflate(R.layout.server_card, viewGroup, false);

        return new ConnectionsHolder(card);
    }

    static abstract class OnTextChangedWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
    }

    @Override
    public void onBindViewHolder(final ConnectionsAdapter.ConnectionsHolder cH, final int i) {
        final Connection connection = mConnections[i];
        cH.mPortNumberView.setText(connection.mServerPort);
        cH.mServerNameView.setText(connection.mServerName);
        cH.mPortNumberView.setText(connection.mServerPort);
        cH.mRemoteSwitch.setChecked(connection.mEnabled);
        cH.mRemoteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                connection.mEnabled = isChecked;
                displayWarningifNoneEnabled();
            }
        });


        cH.mProtoGroup.check(connection.mUseUdp ? R.id.udp_proto : R.id.tcp_proto);
        cH.mProtoGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.udp_proto)
                    connection.mUseUdp=true;
                else if (checkedId == R.id.tcp_proto)
                    connection.mUseUdp=false;
            }
        });

        cH.mCustomOptionsLayout.setVisibility(connection.mUseCustomConfig ? View.VISIBLE : View.GONE);
        cH.mCustomOptionText.setText(connection.mCustomConfiguration);

        cH.mCustomOptionCB.setChecked(connection.mUseCustomConfig);
        cH.mCustomOptionCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                connection.mUseCustomConfig = isChecked;
                cH.mCustomOptionsLayout.setVisibility(connection.mUseCustomConfig ? View.VISIBLE : View.GONE);
                notifyItemChanged(i);
            }
        });

        cH.mDeleteButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder ab = new AlertDialog.Builder(mContext);
                        ab.setTitle(R.string.query_delete_remote);
                        ab.setPositiveButton(R.string.keep, null);
                        ab.setNegativeButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                removeRemote(i);
                                notifyItemRemoved(i);
                            }
                        });
                        ab.create().show();
                    }
                }
        );

        cH.mServerNameView.addTextChangedListener(new OnTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                connection.mServerName = s.toString();
            }

        });

        cH.mPortNumberView.addTextChangedListener(new OnTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                connection.mServerPort = s.toString();
            }
        });

        cH.mCustomOptionText.addTextChangedListener(new OnTextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                connection.mCustomConfiguration = s.toString();
            }
        });

    }

    private void removeRemote(int idx) {
        Connection[] mConnections2 = Arrays.copyOf(mConnections, mConnections.length-1);
        for (int i=idx+1;i<mConnections.length;i++){
            mConnections2[i-1]=mConnections[i];
        }
        mConnections = mConnections2;

    }

    @Override
    public int getItemCount() {
        return mConnections.length;
    }

    public void displayWarningifNoneEnabled()
     {
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
        notifyItemInserted(mConnections.length-1);
        displayWarningifNoneEnabled();
    }

    public void saveProfile() {
        mProfile.mConnections= mConnections;
    }
}
