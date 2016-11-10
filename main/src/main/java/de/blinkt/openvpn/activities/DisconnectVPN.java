/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

/**
 * Created by arne on 13.10.13.
 */
public class DisconnectVPN extends Activity implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {
    private Messenger mMessenger;
    private ServiceConnection mConnection = new ServiceConnection() {



        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
                mMessenger = new Messenger(service);

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mMessenger = null;
        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        showDisconnectDialog();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    private void showDisconnectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_cancel);
        builder.setMessage(R.string.cancel_connection_query);
        builder.setNegativeButton(android.R.string.no, this);
        builder.setPositiveButton(android.R.string.yes, this);
        builder.setNeutralButton(R.string.reconnect, this);
        builder.setOnCancelListener(this);

        builder.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        VpnProfile lastVPN = ProfileManager.getLastConnectedVpn();
        if (which == DialogInterface.BUTTON_POSITIVE) {
            ProfileManager.setConntectedVpnProfileDisconnected(this);
            if (mMessenger != null) {
                Message msg = Message.obtain(null, OpenVPNService.DISCONNECT_VPN_MSG);
                try {
                    mMessenger.send(msg);
                } catch (RemoteException e) {
                    VpnStatus.logException(e);
                }
            }
        } else if (which == DialogInterface.BUTTON_NEUTRAL && lastVPN !=null) {
            Intent intent = new Intent(this, LaunchVPN.class);
            intent.putExtra(LaunchVPN.EXTRA_KEY, lastVPN.getUUID().toString());
            intent.setAction(Intent.ACTION_MAIN);
            startActivity(intent);
        }
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        finish();
    }
}
