package de.blinkt.openvpn;

import java.util.LinkedList;
import java.util.Vector;

import android.content.Context;

public class OpenVPN {
	
	static class LogItem {
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

		String getString(Context c) {
			if(mMessage !=null) {
				return mMessage;
			} else {
				if(mArgs == null)
					return c.getString(mRessourceId);
				else
					return c.getString(mRessourceId,mArgs);
			}
		}
	}
	
	private static final int MAXLOGENTRIES = 200;


	public static final String MANAGMENT_PREFIX = "M:";


	


	public static LinkedList<LogItem> logbuffer = new LinkedList<LogItem>();
	
	private static Vector<LogListener> logListener=new Vector<OpenVPN.LogListener>();
	private static Vector<StateListener> stateListener=new Vector<OpenVPN.StateListener>();
	private static String[] mBconfig;

	public interface LogListener {
		void newLog(LogItem logItem);
	}
	
	public interface StateListener {
		void updateState(String logmessage);
	}

	synchronized static void logMessage(int level,String prefix, String message)
	{
		newlogItem(new LogItem(prefix +  message));

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

	
	synchronized static void addSpeedListener(StateListener sl){
		stateListener.add(sl);
	}

	synchronized static void removeSpeedListener(StateListener sl) {
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

	public static void updateStateString(String msg) {
		for (StateListener sl : stateListener) {
			sl.updateState(msg);
		}
	}

	public static void logInfo(String message) {
		
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

	
	
}
