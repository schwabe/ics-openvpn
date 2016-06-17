/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;

import java.util.Arrays;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;

public class ConnectionsAdapter extends RecyclerView.Adapter<ConnectionsAdapter.ConnectionsHolder> {
    private static final String TAG = ConnectionsAdapter.class.getSimpleName();
    private final Context mContext;
    private final VpnProfile mProfile;
    private final Settings_Connections mConnectionFragment;
    private Connection[] mConnections;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_FOOTER = TYPE_NORMAL + 1;

    public ConnectionsAdapter(Context c, Settings_Connections connections_fragments, VpnProfile vpnProfile) {
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
        private final EditText mConnectText;
        private final SeekBar mConnectSlider;
        private final ConnectionsAdapter mConnectionsAdapter;
        private Connection mConnection;

        public ConnectionsHolder(View card, ConnectionsAdapter connectionsAdapter) {
            super(card);
            mServerNameView = (EditText) card.findViewById(R.id.servername);
            mPortNumberView = (EditText) card.findViewById(R.id.portnumber);
            mRemoteSwitch = (Switch) card.findViewById(R.id.remoteSwitch);
            mCustomOptionCB = (CheckBox) card.findViewById(R.id.use_customoptions);
            mCustomOptionText = (EditText) card.findViewById(R.id.customoptions);
            mProtoGroup = (RadioGroup) card.findViewById(R.id.udptcpradiogroup);
            mCustomOptionsLayout = card.findViewById(R.id.custom_options_layout);
            mDeleteButton = (ImageButton) card.findViewById(R.id.remove_connection);
            mConnectSlider = (SeekBar) card.findViewById(R.id.connect_silder);
            mConnectText = (EditText) card.findViewById(R.id.connect_timeout);

            mConnectionsAdapter = connectionsAdapter;


            /* This is totally weird */
            if (mRemoteSwitch!=null)
                addListeners();
        }

        public void addListeners() {
            mRemoteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mConnection.mEnabled = isChecked;
                    mConnectionsAdapter.displayWarningIfNoneEnabled();
                }
            });

            mProtoGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.udp_proto)
                        mConnection.mUseUdp = true;
                    else if (checkedId == R.id.tcp_proto)
                        mConnection.mUseUdp = false;
                }
            });
            mCustomOptionCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mConnection.mUseCustomConfig = isChecked;
                    mCustomOptionsLayout.setVisibility(mConnection.mUseCustomConfig ? View.VISIBLE : View.GONE);
                }
            });



            mServerNameView.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    mConnection.mServerName = s.toString();
                }

            });

            mPortNumberView.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    mConnection.mServerPort = s.toString();
                }
            });

            mCustomOptionText.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    mConnection.mCustomConfiguration = s.toString();
                }
            });

            mConnectSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mConnectText.setText(String.valueOf(progress));
                        mConnection.mConnectTimeout = progress;
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            mConnectText.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int t = Integer.valueOf(String.valueOf(s));
                        mConnectSlider.setProgress(t);
                        mConnection.mConnectTimeout = t;
                    } catch (Exception ignored) {

                    }
                }
            });


        }
    }


    @Override
    public ConnectionsAdapter.ConnectionsHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater li = LayoutInflater.from(mContext);

        View card;
        if (viewType == TYPE_NORMAL) {
            card = li.inflate(R.layout.server_card, viewGroup, false);
            final ConnectionsHolder H = new ConnectionsHolder(card, this);
            H.mServerNameView.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    H.mConnection.mServerName = s.toString();
                }

            });

            H.mPortNumberView.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    H.mConnection.mServerPort = s.toString();
                }
            });

            H.mCustomOptionText.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    H.mConnection.mCustomConfiguration = s.toString();
                }
            });
            H.mConnectText.addTextChangedListener(new OnTextChangedWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int t = Integer.valueOf(String.valueOf(s));
                        H.mConnectSlider.setProgress(t);
                        H.mConnection.mConnectTimeout = t;
                    } catch (Exception ignored) {

                    }
                }
            });
            return H;
        } else { // TYPE_FOOTER
            card = li.inflate(R.layout.server_footer, viewGroup, false);
        }
        return new ConnectionsHolder(card, this);

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
    public void onBindViewHolder(final ConnectionsAdapter.ConnectionsHolder cH, final int position) {
        if (position == mConnections.length) {
            // Footer
            return;
        }
        final Connection connection = mConnections[position];

        cH.mConnection = connection;

        cH.mPortNumberView.setText(connection.mServerPort);
        cH.mServerNameView.setText(connection.mServerName);
        cH.mPortNumberView.setText(connection.mServerPort);
        cH.mRemoteSwitch.setChecked(connection.mEnabled);

        if (connection.mConnectTimeout==0) {
            cH.mConnectText.setText("");
        } else {
            cH.mConnectText.setText(String.valueOf(connection.mConnectTimeout));
        }
        cH.mConnectSlider.setProgress(connection.mConnectTimeout);


        cH.mProtoGroup.check(connection.mUseUdp ? R.id.udp_proto : R.id.tcp_proto);

        cH.mCustomOptionsLayout.setVisibility(connection.mUseCustomConfig ? View.VISIBLE : View.GONE);
        cH.mCustomOptionText.setText(connection.mCustomConfiguration);

        cH.mCustomOptionCB.setChecked(connection.mUseCustomConfig);

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
                                removeRemote(position);
                                notifyItemRemoved(position);
                            }
                        });
                        ab.create().show();
                    }
                }
        );

        cH.mConnectSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    cH.mConnectText.setText(String.valueOf(progress));
                    connection.mConnectTimeout = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public static void dumpConnection(Connection[] mConnections) {

        for (int i = 0 ; i < mConnections.length && mConnections[i] != null; i++){
            Log.d(TAG, "connection: " + i + ":" + mConnections[i].mServerName);
        }
    }

    private void removeRemote(int idx) {
        Connection[] mConnections2 = Arrays.copyOf(mConnections, mConnections.length - 1);
        for (int i = idx + 1; i < mConnections.length; i++) {
            mConnections2[i - 1] = mConnections[i];
        }
        mConnections = mConnections2;

    }

    @Override
    public int getItemCount() {
        return mConnections.length + 1; //for footer
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mConnections.length)
            return TYPE_FOOTER;
        else
            return TYPE_NORMAL;
    }

    public void addRemote() {
        mConnections = Arrays.copyOf(mConnections, mConnections.length + 1);
        mConnections[mConnections.length - 1] = getDefaultConnection();
        notifyItemInserted(mConnections.length - 1);
        displayWarningIfNoneEnabled();
    }

    public Connection getDefaultConnection(){
        Connection defaultConn = new Connection();
        if (mConnections.length > 0) {
            for (int i = mConnections.length - 1 ; i >= 0 ; i--){
                Connection c = mConnections[i];
                if (null != c){
                    defaultConn = new Connection(c);
                    break;
                }
            }
        }

        return defaultConn;
    }

    protected void displayWarningIfNoneEnabled() {
        int showWarning = View.VISIBLE;
        for (Connection conn : mConnections) {
            if (conn.mEnabled)
                showWarning = View.GONE;
        }
        mConnectionFragment.setWarningVisible(showWarning);
    }


    public void saveProfile() {
        mProfile.mConnections = mConnections;
    }
}
