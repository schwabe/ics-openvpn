package de.blinkt.openvpn.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import de.blinkt.openvpn.R;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Vector;

public class OpenVPN {


	public static LinkedList<LogItem> logbuffer;

	private static Vector<LogListener> logListener;
	private static Vector<StateListener> stateListener;
	private static Vector<ByteCountListener> byteCountListener;

	private static String mLaststatemsg="";

	private static String mLaststate = "NOPROCESS";

	private static int mLastStateresid=R.string.state_noprocess;

	private static long mlastByteCount[]={0,0,0,0};



    public enum ConnectionStatus {
        LEVEL_CONNECTED,
        LEVEL_VPNPAUSED,
        LEVEL_CONNECTING_SERVER_REPLIED,
        LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
        LEVEL_NONETWORK,
		LEVEL_NOTCONNECTED,
		LEVEL_AUTH_FAILED,
		LEVEL_WAITING_FOR_USER_INPUT,
		UNKNOWN_LEVEL
    }

	public static final byte[] officalkey = {-58, -42, -44, -106, 90, -88, -87, -88, -52, -124, 84, 117, 66, 79, -112, -111, -46, 86, -37, 109};
	public static final byte[] officaldebugkey = {-99, -69, 45, 71, 114, -116, 82, 66, -99, -122, 50, -70, -56, -111, 98, -35, -65, 105, 82, 43};
	public static final byte[] amazonkey = {-116, -115, -118, -89, -116, -112, 120, 55, 79, -8, -119, -23, 106, -114, -85, -56, -4, 105, 26, -57};

	private static ConnectionStatus mLastLevel=ConnectionStatus.LEVEL_NOTCONNECTED;

	static {
		logbuffer  = new LinkedList<LogItem>();
		logListener = new Vector<OpenVPN.LogListener>();
		stateListener = new Vector<OpenVPN.StateListener>();
		byteCountListener = new Vector<OpenVPN.ByteCountListener>();
		logInformation();
	}


	public static class LogItem implements Parcelable {
		public static final int ERROR = 1;
		public static final int INFO = 2;
		public static final int VERBOSE = 3;

		private Object [] mArgs = null;
		private String mMessage = null;
		private int mRessourceId;
		// Default log priority
		int mLevel = INFO;
		private long logtime = System.currentTimeMillis();

		public LogItem(int ressourceId, Object[] args) {
			mRessourceId = ressourceId;
			mArgs = args;
		}

		@Override
		public int describeContents() {
			return 0;
		}


		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeArray(mArgs);
			dest.writeString(mMessage);
			dest.writeInt(mRessourceId);
			dest.writeInt(mLevel);
			dest.writeLong(logtime);
		}

		public LogItem(Parcel in) {
			mArgs = in.readArray(Object.class.getClassLoader());
			mMessage = in.readString();
			mRessourceId = in.readInt();
			mLevel = in.readInt();
			logtime = in.readLong();
		}

		public static final Parcelable.Creator<LogItem> CREATOR
		= new Parcelable.Creator<LogItem>() {
			public LogItem createFromParcel(Parcel in) {
				return new LogItem(in);
			}

			public LogItem[] newArray(int size) {
				return new LogItem[size];
			}
		};

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
					if(mRessourceId==R.string.mobile_info)
						return getMobileInfoString(c);
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
		
		// The lint is wrong here 
		@SuppressLint("StringFormatMatches")
		private String getMobileInfoString(Context c) {
			c.getPackageManager();
			String apksign="error getting package signature";

			String version="error getting version";
			try {
				Signature raw = c.getPackageManager().getPackageInfo(c.getPackageName(), PackageManager.GET_SIGNATURES).signatures[0];
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(raw.toByteArray()));
				MessageDigest md = MessageDigest.getInstance("SHA-1");
				byte[] der = cert.getEncoded();
				md.update(der);
				byte[] digest = md.digest();

				if (Arrays.equals(digest, officalkey))
					apksign = c.getString(R.string.official_build);
				else if (Arrays.equals(digest, officaldebugkey))
					apksign = c.getString(R.string.debug_build);
				else if (Arrays.equals(digest, amazonkey))
					apksign = "amazon version";
				else
					apksign = c.getString(R.string.built_by,cert.getSubjectX500Principal().getName());

				PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
				version = packageinfo.versionName;

			} catch (NameNotFoundException e) {
			} catch (CertificateException e) {
			} catch (NoSuchAlgorithmException e) {
			}

			Object[] argsext = Arrays.copyOf(mArgs, mArgs.length+2);
			argsext[argsext.length-1]=apksign;
			argsext[argsext.length-2]=version;

			return c.getString(R.string.mobile_info_extended, argsext);

		}

		public long getLogtime() {
			return logtime;
		}


	}

	private static final int MAXLOGENTRIES = 500;

	public static final String MANAGMENT_PREFIX = "M:";


	public interface LogListener {
		void newLog(LogItem logItem);
	}

	public interface StateListener {
		void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level);
	}

	public interface ByteCountListener {
		void updateByteCount(long in, long out, long diffin, long diffout);
	}

	public synchronized static void logMessage(int level,String prefix, String message)
	{
		newlogItem(new LogItem(prefix +  message));

	}

	public synchronized static void clearLog() {
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

	public synchronized static void addByteCountListener(ByteCountListener bcl) {
		bcl.updateByteCount(mlastByteCount[0],	mlastByteCount[1], mlastByteCount[2], mlastByteCount[3]);
		byteCountListener.add(bcl);
	}	

	public synchronized static void removeByteCountListener(ByteCountListener bcl) {
		byteCountListener.remove(bcl);
	}


	public synchronized static void addStateListener(StateListener sl){
		if(!stateListener.contains(sl)){
			stateListener.add(sl);
			if(mLaststate!=null)
				sl.updateState(mLaststate, mLaststatemsg, mLastStateresid, mLastLevel);
		}
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
		else if (state.equals("DISCONNECTED"))
			return R.string.state_disconnected;
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

    public static void updateStatePause(OpenVPNManagement.pauseReason pauseReason) {
        switch (pauseReason) {
            case noNetwork:
                OpenVPN.updateStateString("NONETWORK", "", R.string.state_nonetwork, ConnectionStatus.LEVEL_NONETWORK);
                break;
            case screenOff:
                OpenVPN.updateStateString("SCREENOFF", "", R.string.state_screenoff, ConnectionStatus.LEVEL_VPNPAUSED);
                break;
            case userPause:
                OpenVPN.updateStateString("USERPAUSE", "", R.string.state_userpause, ConnectionStatus.LEVEL_VPNPAUSED);
                break;
        }

    }

    private static ConnectionStatus getLevel(String state){
		String[] noreplyet = {"CONNECTING","WAIT", "RECONNECTING", "RESOLVE", "TCP_CONNECT"}; 
		String[] reply = {"AUTH","GET_CONFIG","ASSIGN_IP","ADD_ROUTES"};
		String[] connected = {"CONNECTED"};
		String[] notconnected = {"DISCONNECTED", "EXITING"};

		for(String x:noreplyet)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;

		for(String x:reply)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

		for(String x:connected)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_CONNECTED;

		for(String x:notconnected)
			if(state.equals(x))
				return ConnectionStatus.LEVEL_NOTCONNECTED;

		return ConnectionStatus.UNKNOWN_LEVEL;

	}




	public synchronized static void removeStateListener(StateListener sl) {
		stateListener.remove(sl);
	}


	synchronized public static LogItem[] getlogbuffer() {

		// The stoned way of java to return an array from a vector
		// brought to you by eclipse auto complete
		return logbuffer.toArray(new LogItem[logbuffer.size()]);

	}

	public static void updateStateString (String state, String msg) {
		int rid = getLocalizedState(state);
		ConnectionStatus level = getLevel(state);
		updateStateString(state, msg, rid, level);
	}

	public synchronized static void updateStateString(String state, String msg, int resid, ConnectionStatus level) {
		mLaststate= state;
		mLaststatemsg = msg;
		mLastStateresid = resid;
		mLastLevel = level;

		for (StateListener sl : stateListener) {
			sl.updateState(state,msg,resid,level);
		}
	}

	public static void logInfo(String message) {
		newlogItem(new LogItem(LogItem.INFO, message));
	}

	public static void logInfo(int ressourceId, Object... args) {
		newlogItem(new LogItem(LogItem.INFO, ressourceId, args));
	}

	private synchronized static void newlogItem(LogItem logItem) {
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

	public static synchronized void updateByteCount(long in, long out) {
		long lastIn = mlastByteCount[0];
		long lastOut = mlastByteCount[1];
		long diffin = mlastByteCount[2] = in - lastIn;
		long diffout = mlastByteCount[3] = out - lastOut;
		
		

		mlastByteCount = new long[] {in,out,diffin,diffout};
		for(ByteCountListener bcl:byteCountListener){
			bcl.updateByteCount(in, out, diffin,diffout);
		}
	}



}
