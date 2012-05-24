package de.blinkt.openvpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import android.util.Log;

public class OpenVPNThread implements Runnable {
	private static final String TAG = "OpenVPN";
	private String[] mArgv;
	private Process mProcess;
	private String mNativeDir;

	public OpenVPNThread(OpenVpnService service,String[] argv, String nativelibdir)
	{
		mArgv = argv;
		mNativeDir = nativelibdir;
	}
	
	public void stopProcess() {
		mProcess.destroy();
	}

	@Override
	public void run() {
		try {
			Log.i(TAG, "Starting openvpn");			
			startOpenVPNThreadArgs(mArgv);
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


			OpenVPN.updateStateString("No process running");
			// Not a good place to do it, but will do
			OpenVPN.logBuilderConfig(null);
			Log.i(TAG, "Exiting");
		}
	}

	private void startOpenVPNThreadArgs(String[] argv) {
		LinkedList<String> argvlist = new LinkedList<String>();
		
		for(String arg:argv)
			argvlist.add(arg);
	
		ProcessBuilder pb = new ProcessBuilder(argvlist);
		// Hack O rama
		
		// Hack until I find a good way to get the real library path
		String applibpath = argv[0].replace("/cache/minivpn", "/lib");
		
		String lbpath = pb.environment().get("LD_LIBRARY_PATH");
		if(lbpath==null) 
			lbpath = applibpath;
		else
			lbpath = lbpath + ":" + applibpath;
		
		if (!applibpath.equals(mNativeDir)) {
			lbpath = lbpath + ":" + mNativeDir;
		}
		
		pb.environment().put("LD_LIBRARY_PATH", lbpath);
		pb.redirectErrorStream(true);
		try {
			mProcess = pb.start();
			// Close the output, since we don't need it
			mProcess.getOutputStream().close();
			InputStream in = mProcess.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			
			
			while(true) {
				String logline = br.readLine();
				if(logline==null)
					return;
				OpenVPN.logMessage(0, "P:", logline);
			}
		
		
		} catch (IOException e) {
			OpenVPN.logMessage(0, "", "Error reading from output of OpenVPN process"+ e.getLocalizedMessage());
			e.printStackTrace();
			stopProcess();
		}
		
		
	}
}
