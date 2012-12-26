package de.blinkt.openvpn;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.preference.PreferenceManager;

public class NetworkSateReceiver extends BroadcastReceiver {
	private int lastNetwork=-1;
	private OpenVpnManagementThread mManangement;

	private String lastStateMsg=null;
	
	public NetworkSateReceiver(OpenVpnManagementThread managementThread) {
		super();
		mManangement = managementThread;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		NetworkInfo networkInfo = getCurrentNetworkInfo(context);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);        
		boolean sendusr1 = prefs.getBoolean("netchangereconnect", true);

		String netstatestring;
		if(networkInfo==null)
			netstatestring = "not connected";
		else  {
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
			
				if(sendusr1 && lastNetwork!=newnet)
					mManangement.reconnect();
							
				lastNetwork = newnet;
		} else if (networkInfo==null) {
			// Not connected, stop openvpn, set last connected network to no network
			lastNetwork=-1;
			if(sendusr1)
				mManangement.signalusr1();
		}
		
		if(!netstatestring.equals(lastStateMsg))
			OpenVPN.logInfo(R.string.netstatus, netstatestring);
		lastStateMsg=netstatestring;

	}

	private NetworkInfo getCurrentNetworkInfo(Context context) {
		ConnectivityManager conn =  (ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = conn.getActiveNetworkInfo();
		return networkInfo;
	}

}
