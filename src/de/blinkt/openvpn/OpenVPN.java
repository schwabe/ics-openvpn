package de.blinkt.openvpn;

import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;


import android.content.Context;
import android.os.Build;

public class OpenVPN {
	private static final String NOPROCESS = "NOPROCESS";


	public static LinkedList<LogItem> logbuffer;

	private static Vector<LogListener> logListener;
	private static Vector<StateListener> stateListener;
	private static String[] mBconfig;

	private static String mLaststatemsg;

	private static String mLaststate=NOPROCESS;

	private static int mLastStateresid=R.string.state_noprocess;

	static {
		logbuffer  = new LinkedList<LogItem>();
		logListener = new Vector<OpenVPN.LogListener>();
		stateListener = new Vector<OpenVPN.StateListener>();
		logInformation();
	}

	public static class LogItem {
		public static final int ERROR = 1;
		public static final int INFO = 2;
		public static final int VERBOSE = 3;

		private Object [] mArgs = null;
		private String mMessage = null;
		private int mRessourceId;
		// Default log priority
		int mLevel = INFO;

		public LogItem(int ressourceId, Object[] args) {
			mRessourceId = ressourceId;
			mArgs = args;
		}


		public LogItem(int loglevel,int ressourceId, Object[] args) {
			mRessourceId = ressourceId;
			mArgs = args;
			mLevel = loglevel;
		}


		public LogItem(String message) {
			mMessage = message;
		}

		public LogItem(int loglevel, String msg) {
			mLevel = loglevel;
			mMessage = msg;
		}


		public LogItem(int loglevel, int ressourceId) {
			mRessourceId =ressourceId;
			mLevel = loglevel;
		}


		public String getString(Context c) {
			if(mMessage !=null) {
				return mMessage;
			} else {
				if(c!=null) {
					if(mArgs == null)
						return c.getString(mRessourceId);
					else
						return c.getString(mRessourceId,mArgs);
				} else {
					String str = String.format(Locale.ENGLISH,"Log (no context) resid %d", mRessourceId);
					if(mArgs !=null)
						for(Object o:mArgs)
							str += "|" +  o.toString();
					return str;
				}
			}
		}
	}

	private static final int MAXLOGENTRIES = 500;


	public static final String MANAGMENT_PREFIX = "M:";






	public interface LogListener {
		void newLog(LogItem logItem);
	}

	public interface StateListener {
		void updateState(String state, String logmessage, int localizedResId);
	}

	synchronized static void logMessage(int level,String prefix, String message)
	{
		newlogItem(new LogItem(prefix +  message));

	}

	synchronized static void clearLog() {
		logbuffer.clear();
		logInformation();
	}

	private static void logInformation() {

		logInfo(R.string.mobile_info,Build.MODEL, Build.BOARD,Build.BRAND,Build.VERSION.SDK_INT);
	}

	public synchronized static void addLogListener(LogListener ll){
		logListener.add(ll);
	}

	public synchronized static void removeLogListener(LogListener ll) {
		logListener.remove(ll);
	}


	public synchronized static void addStateListener(StateListener sl){
		stateListener.add(sl);
		if(mLaststate!=null)
			sl.updateState(mLaststate, mLaststatemsg, mLastStateresid);
	}	

	private static int getLocalizedState(String state){
		if (state.equals("CONNECTING")) 
			return R.string.state_connecting;
		else if (state.equals("WAIT"))
			return R.string.state_wait;
		else if (state.equals("AUTH"))
			return R.string.state_auth;
		else if (state.equals("GET_CONFIG"))
			return R.string.state_get_config;
		else if (state.equals("ASSIGN_IP"))
			return R.string.state_assign_ip;
		else if (state.equals("ADD_ROUTES"))
			return R.string.state_add_routes;
		else if (state.equals("CONNECTED"))
			return R.string.state_connected;
		else if (state.equals("RECONNECTING"))
			return R.string.state_reconnecting;
		else if (state.equals("EXITING"))
			return R.string.state_exiting;
		else if (state.equals("RESOLVE"))
			return R.string.state_resolve;
		else if (state.equals("TCP_CONNECT"))
			return R.string.state_tcp_connect;
		else
			return R.string.unknown_state;

	}

	public synchronized static void removeStateListener(StateListener sl) {
		stateListener.remove(sl);
	}


	synchronized public static LogItem[] getlogbuffer() {

		// The stoned way of java to return an array from a vector
		// brought to you by eclipse auto complete
		return (LogItem[]) logbuffer.toArray(new LogItem[logbuffer.size()]);

	}
	public static void logBuilderConfig(String[] bconfig) {
		mBconfig = bconfig;
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

	public static void updateStateString (String state, String msg) {
		int rid = getLocalizedState(state);
		updateStateString(state, msg,rid);
	}

	public synchronized static void updateStateString(String state, String msg, int resid) {
		if (! "BYTECOUNT".equals(state)) {
			mLaststate= state;
			mLaststatemsg = msg;
			mLastStateresid = resid;
		}

		for (StateListener sl : stateListener) {
			sl.updateState(state,msg,resid);
		}
	}

	public static void logInfo(String message) {
		newlogItem(new LogItem(LogItem.INFO, message));
	}

	public static void logInfo(int ressourceId, Object... args) {
		newlogItem(new LogItem(LogItem.INFO, ressourceId, args));
	}

	private static void newlogItem(LogItem logItem) {
		logbuffer.addLast(logItem);
		if(logbuffer.size()>MAXLOGENTRIES)
			logbuffer.removeFirst();

		for (LogListener ll : logListener) {
			ll.newLog(logItem);
		}
	}

	public static void logError(String msg) {
		newlogItem(new LogItem(LogItem.ERROR, msg));

	}

	public static void logError(int ressourceId) {
		newlogItem(new LogItem(LogItem.ERROR, ressourceId));
	}
	public static void logError(int ressourceId, Object... args) {
		newlogItem(new LogItem(LogItem.ERROR, ressourceId,args));
	}

}
