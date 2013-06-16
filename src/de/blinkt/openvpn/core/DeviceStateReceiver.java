package de.blinkt.openvpn.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.preference.PreferenceManager;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.core.OpenVPN.ByteCountListener;

import java.util.LinkedList;

public class DeviceStateReceiver extends BroadcastReceiver implements ByteCountListener {
    private int lastNetwork=-1;
    private OpenVPNManagement mManangement;

    // Window time in s
    private final int TRAFFIC_WINDOW = 60;
    // Data traffic limit in bytes
    private final long TRAFFIC_LIMIT = 64 * 1024;


    connectState network= connectState.DISCONNECTED;
    connectState screen = connectState.SHOULDBECONNECTED;

    private String lastStateMsg=null;

    enum connectState {
        SHOULDBECONNECTED,
        PENDINGDISCONNECT,
        DISCONNECTED
    }

    static class Datapoint {
        private Datapoint(long t, long d)
        {
            timestamp = t;
            data = d;
        }

        long timestamp;
        long data;
    }

    LinkedList<Datapoint> trafficdata = new LinkedList<DeviceStateReceiver.Datapoint>();

    @Override
    public void updateByteCount(long in, long out, long diffin, long diffout) {
        if (screen!=connectState.PENDINGDISCONNECT)
            return;

        long total = diffin + diffout;
        trafficdata.add(new Datapoint(System.currentTimeMillis(),total));

        while(trafficdata.getFirst().timestamp <= (System.currentTimeMillis() - TRAFFIC_WINDOW*1000)) {
            trafficdata.removeFirst();
        }

        long windowtraffic = 0;
        for (Datapoint dp: trafficdata)
            windowtraffic += dp.data;

		if(windowtraffic < TRAFFIC_LIMIT) {
			screen = connectState.DISCONNECTED;
			OpenVPN.logInfo(R.string.screenoff_pause,
					OpenVpnService.humanReadableByteCount(TRAFFIC_LIMIT, false), TRAFFIC_WINDOW);

			mManangement.pause();
		}
    }



    public DeviceStateReceiver(OpenVPNManagement magnagement) {
        super();
        mManangement = magnagement;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);


        if(ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            networkStateChange(context);
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            boolean screenoff = prefs.getBoolean("screenoff", false);

            if(screenoff) {
                if(!ProfileManager.getLastConnectedVpn().mPersistTun)
                    OpenVPN.logError(R.string.screen_nopersistenttun);

                screen = connectState.PENDINGDISCONNECT;
                fillTrafficData();
                if (network == connectState.DISCONNECTED)
                    screen = connectState.DISCONNECTED;
            }
        } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            // Network was disabled because screen off
            if (screen == connectState.DISCONNECTED && network == connectState.SHOULDBECONNECTED) {
                mManangement.resume();

            }
            screen = connectState.SHOULDBECONNECTED;

        }
    }

    private void fillTrafficData() {
        trafficdata.add(new Datapoint(System.currentTimeMillis(), TRAFFIC_LIMIT));
    }


    public void networkStateChange(Context context) {
        NetworkInfo networkInfo = getCurrentNetworkInfo(context);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sendusr1 = prefs.getBoolean("netchangereconnect", true);


        String netstatestring;
        if(networkInfo==null) {
            netstatestring = "not connected";
        } else  {
            String subtype = networkInfo.getSubtypeName();
            if(subtype==null)
                subtype = "";
            String extrainfo = networkInfo.getExtraInfo();
            if(extrainfo==null)
                extrainfo="";

			/*
			if(networkInfo.getType()==android.net.ConnectivityManager.TYPE_WIFI) {
				WifiManager wifiMgr = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiinfo = wifiMgr.getConnectionInfo();
				extrainfo+=wifiinfo.getBSSID();

				subtype += wifiinfo.getNetworkId();
			}*/



            netstatestring = String.format("%2$s %4$s to %1$s %3$s",networkInfo.getTypeName(),
                    networkInfo.getDetailedState(),extrainfo,subtype );
        }

        if(networkInfo!=null && networkInfo.getState() == State.CONNECTED) {
            int newnet = networkInfo.getType();
            network = connectState.SHOULDBECONNECTED;

            if(sendusr1 && lastNetwork!=newnet) {
                if (screen == connectState.PENDINGDISCONNECT)
                    screen = connectState.DISCONNECTED;

                if (lastNetwork==-1){
                    if (screen == connectState.SHOULDBECONNECTED)
                        mManangement.resume();
                }else{
                    if (screen == connectState.SHOULDBECONNECTED)
                        mManangement.reconnect();
                }


                lastNetwork = newnet;
            }
        } else if (networkInfo==null) {
            // Not connected, stop openvpn, set last connected network to no network
            lastNetwork=-1;
            if(sendusr1) {
                mManangement.pause();
                network = connectState.DISCONNECTED;

                // Set screen state to be disconnected if it want to disconnect
                if (screen == connectState.PENDINGDISCONNECT)
                    screen = connectState.DISCONNECTED;
            }
        }


        if(!netstatestring.equals(lastStateMsg))
            OpenVPN.logInfo(R.string.netstatus, netstatestring);
        lastStateMsg=netstatestring;

    }

    private NetworkInfo getCurrentNetworkInfo(Context context) {
        ConnectivityManager conn =  (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        return conn.getActiveNetworkInfo();
    }
}
