/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.Preferences;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;

public class RemoteAction extends Activity {

    public static final String EXTRA_NAME = "de.blinkt.openvpn.api.profileName";
    public static final String EXTRA_SERVER_ADDRESS = "de.blinkt.openvpn.api.serverAddress";
    public static final String EXTRA_SERVER_PORT = "de.blinkt.openvpn.api.serverPort";
    public static final String EXTRA_PROTOCOL = "de.blinkt.openvpn.api.protocol";
    private boolean mDoDisconnect;
    private IOpenVPNServiceInternal mService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
            try {
                performAction();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //mService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

    }

    private void connectVPN(Intent intent) {
        String vpnName = intent.getStringExtra(EXTRA_NAME);
        VpnProfile profile = ProfileManager.getInstance(this).getProfileByName(vpnName);
        if (profile == null) {
            Toast.makeText(this, String.format("Vpn profile %s from API call not found", vpnName), Toast.LENGTH_LONG).show();
        } else {
            String serverAddress = intent.getStringExtra(EXTRA_SERVER_ADDRESS);
            String serverPort = intent.getStringExtra(EXTRA_SERVER_PORT);
            String protocol = intent.getStringExtra(EXTRA_PROTOCOL);
            if (profile.mConnections.length == 0
                    || serverAddress != null || serverPort != null || protocol != null) {
                try {
                    profile = createTemporaryProfile(profile, serverAddress, serverPort, protocol);
                    ProfileManager.setTemporaryProfile(this, profile);
                } catch (IllegalArgumentException e) {
                    VpnStatus.logException("Invalid server override from API call", e);
                    Toast.makeText(this, "Invalid server override: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
            }

            Intent startVPN = new Intent(this, LaunchVPN.class);
            startVPN.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
            startVPN.putExtra(OpenVPNService.EXTRA_START_REASON, ".api.ConnectVPN call");
            startVPN.setAction(Intent.ACTION_MAIN);
            startActivity(startVPN);
        }
    }

    static VpnProfile createTemporaryProfile(VpnProfile profile, String serverAddress,
                                             String serverPort, String protocol) {
        VpnProfile temporaryProfile = profile.copy(profile.mName);
        overrideServerSettings(temporaryProfile, serverAddress, serverPort, protocol);
        return temporaryProfile;
    }

    static void overrideServerSettings(VpnProfile profile, String serverAddress,
                                       String serverPort, String protocol) {
        boolean hasNoConnections = profile.mConnections == null || profile.mConnections.length == 0;
        if (hasNoConnections) {
            if (serverAddress == null || serverPort == null || protocol == null)
                throw new IllegalArgumentException("serverAddress, serverPort and protocol are required when profile has no connections");
        } else if (serverAddress == null && serverPort == null && protocol == null) {
            return;
        }

        String validatedAddress = validateServerAddress(serverAddress);
        String validatedPort = validateServerPort(serverPort);
        Boolean useUdp = parseProtocol(protocol);

        if (validatedAddress != null)
            profile.mServerName = validatedAddress;
        if (validatedPort != null)
            profile.mServerPort = validatedPort;
        if (useUdp != null)
            profile.mUseUdp = useUdp;

        Connection firstConnection = hasNoConnections ? new Connection() : profile.mConnections[0];
        if (validatedAddress != null)
            firstConnection.mServerName = validatedAddress;
        if (validatedPort != null)
            firstConnection.mServerPort = validatedPort;
        if (useUdp != null)
            firstConnection.mUseUdp = useUdp;
        profile.mConnections = new Connection[]{firstConnection};
    }

    private static String validateServerAddress(String serverAddress) {
        if (serverAddress == null)
            return null;
        if (serverAddress.isEmpty())
            throw new IllegalArgumentException("serverAddress is empty");

        for (int i = 0; i < serverAddress.length(); i++) {
            char c = serverAddress.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '.' && c != ':' && c != '%'
                    && c != '-' && c != '_')
                throw new IllegalArgumentException("serverAddress contains invalid characters");
        }
        return serverAddress;
    }

    private static String validateServerPort(String serverPort) {
        if (serverPort == null)
            return null;
        if (serverPort.isEmpty())
            throw new IllegalArgumentException("serverPort is empty");

        for (int i = 0; i < serverPort.length(); i++) {
            if (!Character.isDigit(serverPort.charAt(i)))
                throw new IllegalArgumentException("serverPort must be a number");
        }

        final int parsedPort;
        try {
            parsedPort = Integer.parseInt(serverPort);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("serverPort must be a number", e);
        }
        if (parsedPort < 1 || parsedPort > 65535)
            throw new IllegalArgumentException("serverPort must be between 1 and 65535");

        return serverPort;
    }

    private static Boolean parseProtocol(String protocol) {
        if (protocol == null)
            return null;
        if ("udp".equalsIgnoreCase(protocol))
            return true;
        if ("tcp".equalsIgnoreCase(protocol))
            return false;
        throw new IllegalArgumentException("protocol must be tcp or udp");
    }

    private void setDefaultVPN(Intent intent) {
        String defaultVpnName = intent.getStringExtra(EXTRA_NAME);
        VpnProfile defaultProfile = ProfileManager.getInstance(this).getProfileByName(defaultVpnName);
        if (defaultProfile == null) {
            Toast.makeText(this, String.format("Vpn profile %s from API call not found", defaultVpnName), Toast.LENGTH_LONG).show();
        } else {
            SharedPreferences prefs = Preferences.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("alwaysOnVpn", defaultProfile.getUUIDString());
            editor.apply();
        }
    }

    private void performAction() throws RemoteException {

        if (!mService.isAllowedExternalApp(getCallingPackage())) {
            finish();
            return;
        }

        Intent intent = getIntent();
        setIntent(null);
        ComponentName component = intent.getComponent();
        if (component == null)
            return;


        switch (component.getShortClassName()) {
            case ".api.DisconnectVPN":
                mService.stopVPN(false);
                break;
            case ".api.PauseVPN":
                mService.userPause(true);
                break;
            case ".api.ResumeVPN":
                mService.userPause(false);
                break;
            case ".api.ConnectVPN":
                connectVPN(intent);
                break;
            case ".api.SetDefaultVPN":
                setDefaultVPN(intent);
                break;
        }
        finish();
    }

    @Override
    public void finish() {
        if(mService!=null) {
            mService = null;
            getApplicationContext().unbindService(mConnection);
        }
        super.finish();
    }
}
