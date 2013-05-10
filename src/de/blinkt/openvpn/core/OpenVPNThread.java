package de.blinkt.openvpn.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Log;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.OpenVPN.ConnectionStatus;
import de.blinkt.openvpn.core.OpenVPN.LogItem;

public class OpenVPNThread implements Runnable {
	private static final String DUMP_PATH_STRING = "Dump path: ";
	private static final String TAG = "OpenVPN";
	private String[] mArgv;
	private Process mProcess;
	private String mNativeDir;
	private OpenVpnService mService;
	private String mDumpPath;
	private Map<String, String> mProcessEnv;

	public OpenVPNThread(OpenVpnService service,String[] argv, Map<String,String> processEnv, String nativelibdir)
	{
		mArgv = argv;
		mNativeDir = nativelibdir;
		mService = service;
	}
	
	public void stopProcess() {
		mProcess.destroy();
	}

	
	
	@Override
	public void run() {
		try {
			Log.i(TAG, "Starting openvpn");			
			startOpenVPNThreadArgs(mArgv, mProcessEnv);
			Log.i(TAG, "Giving up");
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "OpenVPNThread Got " + e.toString());
		} finally {
			int exitvalue = 0;
			try {
				 exitvalue = mProcess.waitFor();
			} catch ( IllegalThreadStateException ite) {
				OpenVPN.logError("Illegal Thread state: " + ite.getLocalizedMessage());
			} catch (InterruptedException ie) {
				OpenVPN.logError("InterruptedException: " + ie.getLocalizedMessage());
			}
			if( exitvalue != 0)
				OpenVPN.logError("Process exited with exit value " + exitvalue);
			
			OpenVPN.updateStateString("NOPROCESS","No process running.", R.string.state_noprocess,ConnectionStatus.LEVEL_NOTCONNECTED);
			if(mDumpPath!=null) {
				try {
					BufferedWriter logout = new BufferedWriter(new FileWriter(mDumpPath + ".log"));
					SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.GERMAN);
					for(LogItem li :OpenVPN.getlogbuffer()){
						String time = timeformat.format(new Date(li.getLogtime()));
						logout.write(time +" " + li.getString(mService) + "\n");
					}
					logout.close();
					OpenVPN.logError(R.string.minidump_generated);
				} catch (IOException e) {
					OpenVPN.logError("Writing minidump log: " +e.getLocalizedMessage());
				}
			}

			mService.processDied();
			Log.i(TAG, "Exiting");
		}
	}
	
	private void startOpenVPNThreadArgs(String[] argv,Map<String, String> env) {
		LinkedList<String> argvlist = new LinkedList<String>();
		
		for(String arg:argv)
			argvlist.add(arg);
	
		ProcessBuilder pb = new ProcessBuilder(argvlist);
		// Hack O rama
		
		String lbpath = genLibraryPath(argv, pb);
		
		pb.environment().put("LD_LIBRARY_PATH", lbpath);

		// Add extra variables
		for(Entry<String,String> e:env.entrySet()){
			pb.environment().put(e.getKey(), e.getValue());
		}
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

				if (logline.startsWith(DUMP_PATH_STRING))
					mDumpPath = logline.substring(DUMP_PATH_STRING.length());
					

				OpenVPN.logMessage(0, "P:", logline);
			}
			
		
		} catch (IOException e) {
			OpenVPN.logMessage(0, "", "Error reading from output of OpenVPN process"+ e.getLocalizedMessage());
			e.printStackTrace();
			stopProcess();
		}
		
		
	}

	private String genLibraryPath(String[] argv, ProcessBuilder pb) {
		// Hack until I find a good way to get the real library path
		String applibpath = argv[0].replace("/cache/" + VpnProfile.MINIVPN , "/lib");
		
		String lbpath = pb.environment().get("LD_LIBRARY_PATH");
		if(lbpath==null) 
			lbpath = applibpath;
		else
			lbpath = lbpath + ":" + applibpath;
		
		if (!applibpath.equals(mNativeDir)) {
			lbpath = lbpath + ":" + mNativeDir;
		}
		return lbpath;
	}
}
