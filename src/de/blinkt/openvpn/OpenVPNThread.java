package de.blinkt.openvpn;

import java.util.Arrays;

import android.util.Log;

public class OpenVPNThread implements Runnable {
	private static final String TAG = "OpenVPN";
	private OpenVpnService mService;
	private String[] mArgv;

	public OpenVPNThread(OpenVpnService service,String[] argv)
	{
		mService = service;
		mArgv = argv;
	}

	@Override
	public void run() {
		try {
			Log.i(TAG, "Starting o");


			OpenVPN.setCallback(mService);


			// We try to create the tunnel for several times. The better way
			// is to work with ConnectivityManager, such as trying only when
			// the network is avaiable. Here we just use a counter to keep
			// things simple.
			//for (int attempt = 0; attempt < 10; ++attempt) {
			mService.getHandler().sendEmptyMessage(R.string.connecting);

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
				///    mInterface.close();
			} catch (Exception e) {
				// ignore	
			}
			//mInterface = null;


			mService.getHandler().sendEmptyMessage(R.string.disconnected);
			// Not a good place to do it, but will do
			OpenVPN.logBuilderConfig(null);
			Log.i(TAG, "Exiting");
		}
	}
}
