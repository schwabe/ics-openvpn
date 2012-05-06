package de.blinkt.openvpn;

import java.util.LinkedList;
import java.util.Vector;

import android.util.Log;

public class OpenVPN {
	private static OpenVpnService mOpenVpnService;
	private static final int MAXLOGENTRIES = 500;
	public static native int startOpenVPNThreadArgs(String argv[]);
	private static final String TAG = "OpenVpn";


	public static LinkedList<String> logbuffer = new LinkedList<String>();
	private static int counter=0;

	private static Vector<LogListener> logListener=new Vector<OpenVPN.LogListener>();
	private static String[] mBconfig;

	public interface LogListener {
		void newLog(String logmessage);
	}

	static {
		System.loadLibrary("crypto");
		System.loadLibrary("ssl");
		System.loadLibrary("lzo");
		System.loadLibrary("openvpn");
	}

	synchronized static void logMessage(int level,String prefix, String message)
	{
		logbuffer.addLast(prefix +  message);
		if(logbuffer.size()>MAXLOGENTRIES)
			logbuffer.removeFirst();

		// The garbage collector does not collect the String from native
		// but kills me for logging 100 messages with too many references :(
		// Force GC how and then to kill loose ends
		if(counter++ % 50==0) {
			System.gc();
		}

		for (LogListener ll : logListener) {
			ll.newLog(prefix + message);
		}

	}

	synchronized static void clearLog() {
		logbuffer.clear();
	}

	synchronized static void addLogListener(LogListener ll){
		logListener.add(ll);
	}

	synchronized static void removeLogListener(LogListener ll) {
		logListener.remove(ll);
	}




	public static void setCallback(OpenVpnService openVpnService) {
		mOpenVpnService = openVpnService;
	}

	//! Dummy method being called to force loading of JNI Libraries
	public static void foo() {	}

	synchronized public static String[] getlogbuffer() {

		// The stoned way of java to return an array from a vector
		// brought to you by eclipse auto complete
		return (String[]) logbuffer.toArray(new String[logbuffer.size()]);

	}
	public static void logBuilderConfig(String[] bconfig) {
		mBconfig =bconfig;
	}
	public static void triggerLogBuilderConfig() {
		if(mBconfig==null) {
			logMessage(0, "", "No active interface");
		} else {
			for (String item : mBconfig) {
				logMessage(0, "", item);
			}	
		}

	}
}
