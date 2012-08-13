/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.blinkt.openvpn;

import java.io.IOException;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import de.blinkt.openvpn.OpenVPN.StateListener;

public class OpenVpnService extends VpnService implements StateListener {
	private Thread mServiceThread;

	private Vector<String> mDnslist=new Vector<String>();

	private VpnProfile mProfile;

	private String mDomain=null;

	private Vector<CIDRIP> mRoutes=new Vector<CIDRIP>();
	private Vector<String> mRoutesv6=new Vector<String>();

	private CIDRIP mLocalIP=null;

	private OpenVpnManagementThread mSocketManager;

	private Thread mSocketManagerThread;
	private int mMtu;
	private String mLocalIPv6=null;
	private NetworkSateReceiver mNetworkStateReceiver;

	private boolean mDisplayBytecount=false;

	private boolean mNotificationvisible;

	private static final int OPENVPN_STATUS = 1;
	
	@Override
	public void onRevoke() {
		OpenVpnManagementThread.stopOpenVPN();
		mServiceThread=null;
		stopSelf();
		ProfileManager.setConntectedVpnProfileDisconnected(this);
	};

	private void hideNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancel(OPENVPN_STATUS);
		mNotificationvisible=false;
	}
	private void showNotification(String msg, String tickerText) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);


		int icon = R.drawable.ic_stat_vpn;
		long when = System.currentTimeMillis();
		
		android.app.Notification.Builder nbuilder = new Notification.Builder(this);

		nbuilder.setContentTitle("OpenVPN - "  + mProfile.mName);
		nbuilder.setContentText(msg);
		nbuilder.setOnlyAlertOnce(true);
		nbuilder.setOngoing(true);
		nbuilder.setContentIntent(getLogPendingIntent());
		nbuilder.setSmallIcon(icon);
		nbuilder.setWhen(when);
		
		if(tickerText!=null)
			nbuilder.setTicker(tickerText);

		Notification notification = nbuilder.getNotification();

		mNotificationManager.notify(OPENVPN_STATUS, notification);
		mNotificationvisible=true;
		
	}

	PendingIntent getLogPendingIntent() {
		// Let the configure Button show the Log
		Intent intent = new Intent(getBaseContext(),LogWindow.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		PendingIntent startLW = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
		intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		return startLW;

	}



	private LocalServerSocket openManagmentInterface(int tries) {
		// Could take a while to open connection
		String socketname = (getCacheDir().getAbsolutePath() + "/" +  "mgmtsocket");
		LocalSocket sock = new LocalSocket();

		while(tries > 0 && !sock.isConnected()) {
			try {
				sock.bind(new LocalSocketAddress(socketname,
						LocalSocketAddress.Namespace.FILESYSTEM));
			} catch (IOException e) {
				// wait 300 ms before retrying
				try { Thread.sleep(300);
				} catch (InterruptedException e1) {}

			} 
			tries--;
		}
		
		try {
			LocalServerSocket lss = new LocalServerSocket(sock.getFileDescriptor());
			return lss;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		

	}
	
	void registerNetworkStateReceiver() {
		  // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mNetworkStateReceiver = new NetworkSateReceiver(mSocketManager);
        this.registerReceiver(mNetworkStateReceiver, filter);
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Extract information from the intent.
		String prefix = getPackageName();
		String[] argv = intent.getStringArrayExtra(prefix + ".ARGV");
		String nativelibdir = intent.getStringExtra(prefix + ".nativelib");

		String profileUUID = intent.getStringExtra(prefix + ".profileUUID");
		mProfile = ProfileManager.get(profileUUID);
		
		OpenVPN.addSpeedListener(this);
		
		// Stop the previous session by interrupting the thread.
		if(OpenVpnManagementThread.stopOpenVPN()){
			// an old was asked to exit, wait 2s
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}

		if (mServiceThread!=null) {
			mServiceThread.interrupt();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
		}
		// Open the Management Interface
		LocalServerSocket mgmtsocket = openManagmentInterface(8);

		if(mgmtsocket!=null) {
			// start a Thread that handles incoming messages of the managment socket
			mSocketManager = new OpenVpnManagementThread(mProfile,mgmtsocket,this);
			mSocketManagerThread = new Thread(mSocketManager,"OpenVPNMgmtThread");
			mSocketManagerThread.start();
			OpenVPN.logInfo("started Socket Thread");
			registerNetworkStateReceiver();
		}


		// Start a new session by creating a new thread.
		OpenVPNThread serviceThread = new OpenVPNThread(this, argv,nativelibdir);

		mServiceThread = new Thread(serviceThread, "OpenVPNServiceThread");
		mServiceThread.start();

		ProfileManager.setConnectedVpnProfile(this, mProfile);
		
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mServiceThread != null) {
			mSocketManager.managmentCommand("signal SIGINT\n");

			mServiceThread.interrupt();
		}
		  if (mNetworkStateReceiver!= null) {
	            this.unregisterReceiver(mNetworkStateReceiver);
	        }
	}



	public ParcelFileDescriptor openTun() {
		Builder builder = new Builder();

		if(mLocalIP==null && mLocalIPv6==null) {
			OpenVPN.logMessage(0, "", getString(R.string.opentun_no_ipaddr));
			return null;
		}

		if(mLocalIP!=null) {
			builder.addAddress(mLocalIP.mIp, mLocalIP.len);
		}

		if(mLocalIPv6!=null) {
			String[] ipv6parts = mLocalIPv6.split("/");
			builder.addAddress(ipv6parts[0],Integer.parseInt(ipv6parts[1]));
		}


		for (String dns : mDnslist ) {
			try {
				builder.addDnsServer(dns);
			} catch (IllegalArgumentException iae) {
				OpenVPN.logError(R.string.dns_add_error, dns,iae.getLocalizedMessage());
			}
		}
		
		
		builder.setMtu(mMtu);


		for (CIDRIP route:mRoutes) {
			try {
				builder.addRoute(route.mIp, route.len);
			} catch (IllegalArgumentException ia) {
				OpenVPN.logMessage(0, "", getString(R.string.route_rejected) + route + " " + ia.getLocalizedMessage());
			}
		}

		for(String v6route:mRoutesv6) {
			try {
				String[] v6parts = v6route.split("/");
				builder.addRoute(v6parts[0],Integer.parseInt(v6parts[1]));
			} catch (IllegalArgumentException ia) {
				OpenVPN.logMessage(0, "", getString(R.string.route_rejected) + v6route + " " + ia.getLocalizedMessage());
			}
		}

		if(mDomain!=null)
			builder.addSearchDomain(mDomain);

		String bconfig[] = new String[6];

		bconfig[0]= getString(R.string.last_openvpn_tun_config);
		bconfig[1] = String.format(getString(R.string.local_ip_info,mLocalIP.mIp,mLocalIP.len,mLocalIPv6, mMtu));
		bconfig[2] = String.format(getString(R.string.dns_server_info, joinString(mDnslist)));
		bconfig[3] = String.format(getString(R.string.dns_domain_info, mDomain));
		bconfig[4] = String.format(getString(R.string.routes_info, joinString(mRoutes)));
		bconfig[5] = String.format(getString(R.string.routes_info6, joinString(mRoutesv6)));

		String session = mProfile.mName;
		if(mLocalIP!=null)
			session+= " - " + mLocalIP;
		if(mLocalIPv6!=null)
			session+= " - " + mLocalIPv6;
		builder.setSession(session);


		OpenVPN.logBuilderConfig(bconfig);
		
		// No DNS Server, log a warning 
		if(mDnslist.size()==0)
			OpenVPN.logInfo(R.string.warn_no_dns);
			
		// Reset information
		mDnslist.clear();
		mRoutes.clear();
		mRoutesv6.clear();
		mLocalIP=null;
		mLocalIPv6=null;
		mDomain=null;

		builder.setConfigureIntent(getLogPendingIntent());

		try {
			ParcelFileDescriptor pfd = builder.establish();
			return pfd;
		} catch (Exception e) {
			OpenVPN.logMessage(0, "", getString(R.string.tun_open_error));
			OpenVPN.logMessage(0, "", getString(R.string.error) + e.getLocalizedMessage());
			OpenVPN.logMessage(0, "", getString(R.string.tun_error_helpful));
			return null;
		}

	}


	// Ugly, but java has no such method
	private <T> String joinString(Vector<T> vec) {
		String ret = "";
		if(vec.size() > 0){ 
			ret = vec.get(0).toString();
			for(int i=1;i < vec.size();i++) {
				ret = ret + ", " + vec.get(i).toString();
			}
		}
		return ret;
	}






	public void addDNS(String dns) {
		mDnslist.add(dns);		
	}


	public void setDomain(String domain) {
		if(mDomain==null) {
			mDomain=domain;
		}
	}


	public void addRoute(String dest, String mask) {
		CIDRIP route = new CIDRIP(dest, mask);		
		if(route.len == 32 && !mask.equals("255.255.255.255")) {
			OpenVPN.logMessage(0, "", String.format(getString(R.string.route_not_cidr,dest,mask)));
		}

		if(route.normalise())
			OpenVPN.logMessage(0, "", String.format(getString(R.string.route_not_netip,dest,route.len,route.mIp)));

		mRoutes.add(route);
	}

	public void addRoutev6(String extra) {
		mRoutesv6.add(extra);		
	}


	public void setLocalIP(String local, String netmask,int mtu, String mode) {
		mLocalIP = new CIDRIP(local, netmask);
		mMtu = mtu;

		if(mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
			// get the netmask as IP
			long netint = CIDRIP.getInt(netmask);
			if(Math.abs(netint - mLocalIP.getInt()) ==1) {
				if(mode.equals("net30"))
					mLocalIP.len=30;
				else
					mLocalIP.len=31;
			} else {
				OpenVPN.logMessage(0, "", String.format(getString(R.string.ip_not_cidr, local,netmask,mode)));
			}
		}
	}

	public void setLocalIPv6(String ipv6addr) {
		mLocalIPv6 = ipv6addr;
	}

	@Override
	public void updateState(String state,String logmessage) {
		if("NOPROCESS".equals(state)) {
			hideNotification();
			mDisplayBytecount=false;
			return;
		}
		
		if("CONNECTED".equals(state)) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);        
			mDisplayBytecount = prefs.getBoolean("statusafterconnect", false);
			if(!mDisplayBytecount) {
				hideNotification();
				return;
			}			
		}
		
		// Skip exiting status if the status is already hidden
		if(("EXITING SIGINT".equals(state) || "EXITING".equals(state))
				&& !mNotificationvisible) {
			return;
		}	
		
		if("BYTECOUNT".equals(state)) {
			if(mDisplayBytecount) {
				showNotification(logmessage,null);
			}
		} else {
			// Other notifications are shown
			String ticker = state.toLowerCase();
			showNotification(state +" " + logmessage,ticker);
		}
	}

}
