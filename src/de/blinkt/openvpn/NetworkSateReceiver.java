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
			netstatestring = "null";
		else 
			netstatestring = String.format("%2$s %4$s to %1$s %3$s",networkInfo.getTypeName(),
					networkInfo.getDetailedState(),networkInfo.getExtraInfo(), networkInfo.getSubtypeName());
		
		if(networkInfo!=null && networkInfo.getState() == State.CONNECTED) {
				int newnet = networkInfo.getType();
			
				if(sendusr1 && lastNetwork!=-1 && (lastNetwork!=newnet))
					mManangement.reconnect();
							
				lastNetwork = newnet;
		}
		if(!netstatestring.equals(lastStateMsg))
			OpenVPN.logMessage(0, "Network:", netstatestring);
		lastStateMsg=netstatestring;

	}

	private NetworkInfo getCurrentNetworkInfo(Context context) {
		ConnectivityManager conn =  (ConnectivityManager)
				context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo networkInfo = conn.getActiveNetworkInfo();
		return networkInfo;
	}

}
