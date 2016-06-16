/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import java.util.Locale;

import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;


/**
 * Created by arne on 22.04.16.
 */
@TargetApi(Build.VERSION_CODES.N)
public class OpenVPNTileService extends TileService implements VpnStatus.StateListener {

    @SuppressLint("Override")
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onClick() {
        super.onClick();
        final VpnProfile bootProfile = getQSVPN();
        if (bootProfile == null) {
            Toast.makeText(this, R.string.novpn_selected, Toast.LENGTH_SHORT).show();
        } else {
            if (!isLocked())
                clickAction(bootProfile);
            else
                unlockAndRun(new Runnable() {
                    @Override
                    public void run() {
                        clickAction(bootProfile);
                    }
                });
        }
    }

    private void clickAction(VpnProfile bootProfile) {
        if (VpnStatus.isVPNActive()) {
            Intent intent = new Intent(this, OpenVPNService.class);
            intent.setAction(OpenVPNService.START_SERVICE);
            bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder binder) {
                    OpenVPNService service = ((OpenVPNService.LocalBinder) binder).getService();

                    if (service != null && service.getManagement() != null)
                        service.getManagement().stopVPN(false);

                    unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {

                }
            }, Context.BIND_AUTO_CREATE);
        } else
            launchVPN(bootProfile, this);
    }


    @SuppressLint("Override")
    @TargetApi(Build.VERSION_CODES.N)
    void launchVPN(VpnProfile profile, Context context) {
        Intent startVpnIntent = new Intent(Intent.ACTION_MAIN);
        startVpnIntent.setClass(context, LaunchVPN.class);
        startVpnIntent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUIDString());
        startVpnIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startVpnIntent.putExtra(LaunchVPN.EXTRA_HIDELOG, true);

        context.startActivity(startVpnIntent);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onTileAdded() {
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        VpnStatus.addStateListener(this);
    }


    @TargetApi(Build.VERSION_CODES.N)
    public VpnProfile getQSVPN() {
        return ProfileManager.getAlwaysOnVPN(this);
    }

    @Override
    public void updateState(String state, String logmessage, int localizedResId, VpnStatus.ConnectionStatus level) {
        VpnProfile vpn;
        Tile t = getQsTile();
        if (level == VpnStatus.ConnectionStatus.LEVEL_AUTH_FAILED || level == VpnStatus.ConnectionStatus.LEVEL_NOTCONNECTED) {
            // No VPN connected, use stadnard VPN
            vpn = getQSVPN();
            if (vpn == null) {
                t.setLabel(getString(R.string.novpn_selected));
                t.setState(Tile.STATE_UNAVAILABLE);
            } else {
                t.setLabel(getString(R.string.qs_connect, vpn.getName()));
                t.setState(Tile.STATE_INACTIVE);
            }
        } else {
            vpn = ProfileManager.getLastConnectedVpn();
            String name;
            if (vpn == null)
                name = "null?!";
            else
                name = vpn.getName();
            t.setLabel(getString(R.string.qs_disconnect, name));
            t.setState(Tile.STATE_ACTIVE);
        }


        t.updateTile();
    }

    @Override
    public void onStopListening() {
        VpnStatus.removeStateListener(this);
        super.onStopListening();
    }
}
