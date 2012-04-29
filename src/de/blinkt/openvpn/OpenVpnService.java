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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Vector;

import de.blinkt.openvpn.OpenVpnService.CIDRIP;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.VpnService;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

public class OpenVpnService extends VpnService implements Handler.Callback, Runnable {
    private static final String TAG = "OpenVpnService";

    private String[] mArgv;

    private Handler mHandler;
    // Only one VPN, make this thread shared between all instances
    private static Thread mThread;

    private ParcelFileDescriptor mInterface;

	private Vector<String> mDnslist=new Vector<String>();

	private VpnProfile mProfile;

	private String mDomain=null;

	private Vector<CIDRIP> mRoutes=new Vector<CIDRIP>();

	private CIDRIP mLocalIP;

	
	class CIDRIP{
		String mIp;
		int len;
		public CIDRIP(String ip, String mask){
			mIp=ip;
			String[] ipt = mask.split("\\.");
			long netmask=0;
			
			netmask += Integer.parseInt(ipt[0]);
			netmask += Integer.parseInt(ipt[1])<< 8;
			netmask += Integer.parseInt(ipt[2])<< 16;
			netmask += Integer.parseInt(ipt[3])<< 24;
			
			len =0;
			while((netmask & 0x1) == 1) {
				len++;
				netmask = netmask >> 1;
			}
		}
		@Override
		public String toString() {
			return String.format("%s/%d",mIp,len);
		}
	}
	
    @Override
    public void onRevoke() {
    	managmentCommand("signal SIGINT\n");
    	mThread=null;
    	stopSelf();
    };
    
	
	public void managmentCommand(String cmd) {
		LocalSocket mgmtsocket;
		try {
			byte[] buffer = new byte[400];
			mgmtsocket = new LocalSocket();
					
			mgmtsocket.connect(new LocalSocketAddress(getCacheDir().getAbsolutePath() + "/" +  "mgmtsocket", 
					LocalSocketAddress.Namespace.FILESYSTEM)); 
			//mgmtsocket  = new Dat("127.0.0.1",OpenVPNClient.MANAGMENTPORT));
			
			//OutputStreamWriter outw = new OutputStreamWriter(mgmtsocket.getOutputStream());
			mgmtsocket.getInputStream().read(buffer);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			//outw.write(cmd);
			mgmtsocket.getOutputStream().write(cmd.getBytes());
			//outw.flush();
			try {
				Thread.sleep(400);
			} catch (InterruptedException e) {
			}

			mgmtsocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
    
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The handler is only used to show messages.
        if (mHandler == null) {
            mHandler = new Handler(this);
        }

        // Stop the previous session by interrupting the thread.
        if (mThread != null) {
        	managmentCommand("signal SIGINT\n");
            mThread.interrupt();
        }

        // Thread already running, reuse existing,  
        
        // Extract information from the intent.
        String prefix = getPackageName();
        mArgv = intent.getStringArrayExtra(prefix + ".ARGV");
        

        // Start a new session by creating a new thread.
        mThread = new Thread(this, "OpenVPNThread");
        mThread.start();
        
        String profileUUID = intent.getStringExtra(prefix + ".profileUUID");
        mProfile = ProfileManager.get(profileUUID);
        
        if(intent.hasExtra(prefix +".PKCS12PASS"))
        {
        	try {
        		String pkcs12password = intent.getStringExtra(prefix +".PKCS12PASS");
				Thread.sleep(3000);
				
				managmentCommand("password 'Private Key' " + pkcs12password + "\n");
			} catch (InterruptedException e) {
			}
        	
        }
        if(intent.hasExtra(prefix +".USERNAME"))
        {
        	try {
        		String user = managmentEscape(intent.getStringExtra(prefix +".USERNAME"));
        		String pw = managmentEscape(intent.getStringExtra(prefix +".PASSWORD"));
				Thread.sleep(3000);
				
				
				managmentCommand("username 'Auth' " + user+ "\n" + 
						"password 'Auth' " + pw + "\n");
			} catch (InterruptedException e) {
			}
        
        }
        
        
        return START_STICKY;
    }

    private String managmentEscape(String unescape) {
    	String escapedString = unescape.replace("\\", "\\\\");
    	escapedString = escapedString.replace("\"","\\\"");
    	escapedString = escapedString.replace("\n","\\n");
    	return '"' + escapedString + '"';
	}


	@Override
    public void onDestroy() {
        if (mThread != null) {
        	managmentCommand("signal SIGINT\n");

            mThread.interrupt();
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        if (message != null) {
            Toast.makeText(this, message.what, Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public synchronized void run() {
        try {
            Log.i(TAG, "Starting o");

            
            OpenVPN.setCallback(this);


            // We try to create the tunnel for several times. The better way
            // is to work with ConnectivityManager, such as trying only when
            // the network is avaiable. Here we just use a counter to keep
            // things simple.
            //for (int attempt = 0; attempt < 10; ++attempt) {
                mHandler.sendEmptyMessage(R.string.connecting);

                // Log argv
                
                OpenVPN.logMessage(0, "argv:" , Arrays.toString(mArgv));
                
                OpenVPN.startOpenVPNThreadArgs(mArgv);
                
                
                
                // Sleep for a while. This also checks if we got interrupted.
                Thread.sleep(3000);
            //}
            Log.i(TAG, "Giving up");
        } catch (Exception e) {
            Log.e(TAG, "Got " + e.toString());
        } finally {
            try {
                mInterface.close();
            } catch (Exception e) {
                // ignore	
            }
            mInterface = null;
            

            mHandler.sendEmptyMessage(R.string.disconnected);
            Log.i(TAG, "Exiting");
        }
    }


	public ParcelFileDescriptor openTun() {
        Builder builder = new Builder();
        
        builder.addAddress(mLocalIP.mIp, mLocalIP.len);
        
        for (String dns : mDnslist ) {
            builder.addDnsServer(dns);
		}
     
        
        for (CIDRIP route:mRoutes) {
        	builder.addRoute(route.mIp, route.len);
        }
        
        if(mDomain!=null)
        	builder.addSearchDomain(mDomain);
        

        mDnslist.clear();
        mRoutes.clear();

        
        builder.setSession(mProfile.mName + " - " + mLocalIP);
        
        // Let the configure Button show the Log
        Intent intent = new Intent(getBaseContext(),LogWindow.class);
        PendingIntent startLW = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        builder.setConfigureIntent(startLW);
        mInterface = builder.establish();
        return mInterface;

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
		mRoutes.add(new CIDRIP(dest, mask));
	}


	public void setLocalIP(String local, String netmask) {
		mLocalIP = new CIDRIP(local, netmask);
	}
}
