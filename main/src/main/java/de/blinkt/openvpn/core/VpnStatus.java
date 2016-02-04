/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.FormatFlagsConversionMismatchException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UnknownFormatConversionException;
import java.util.Vector;

import de.blinkt.openvpn.R;

public class VpnStatus {


    public static LinkedList<LogItem> logbuffer;

    private static Vector<LogListener> logListener;
    private static Vector<StateListener> stateListener;
    private static Vector<ByteCountListener> byteCountListener;

    private static String mLaststatemsg = "";

    private static String mLaststate = "NOPROCESS";

    private static int mLastStateresid = R.string.state_noprocess;

    private static long mlastByteCount[] = {0, 0, 0, 0};

    public static void logException(LogLevel ll, String context, Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LogItem li;
        if (context != null) {
            li = new LogItem(ll, R.string.unhandled_exception_context, e.getMessage(), sw.toString(), context);
        } else {
            li = new LogItem(ll, R.string.unhandled_exception, e.getMessage(), sw.toString());
        }
        newLogItem(li);
    }

    public static void logException(Exception e) {
        logException(LogLevel.ERROR, null, e);
    }

    public static void logException(String context, Exception e) {
        logException(LogLevel.ERROR, context, e);
    }

    private static final int MAXLOGENTRIES = 1000;


    public static String getLastCleanLogMessage(Context c) {
        String message = mLaststatemsg;
        switch (mLastLevel) {
            case LEVEL_CONNECTED:
                String[] parts = mLaststatemsg.split(",");
                /*
                   (a) the integer unix date/time,
                   (b) the state name,
                   0 (c) optional descriptive string (used mostly on RECONNECTING
                    and EXITING to show the reason for the disconnect),

                    1 (d) optional TUN/TAP local IPv4 address
                   2 (e) optional address of remote server,
                   3 (f) optional port of remote server,
                   4 (g) optional local address,
                   5 (h) optional local port, and
                   6 (i) optional TUN/TAP local IPv6 address.
*/
                // Return only the assigned IP addresses in the UI
                if (parts.length >= 7)
                    message = String.format(Locale.US, "%s %s", parts[1], parts[6]);
                break;
        }

        while (message.endsWith(","))
            message = message.substring(0, message.length() - 1);

        String status = mLaststate;
        if (status.equals("NOPROCESS"))
            return message;

        String prefix = c.getString(mLastStateresid);
        if (mLastStateresid == R.string.unknown_state)
            message = status + message;
        if (message.length() > 0)
            prefix += ": ";

        return prefix + message;

    }

    public static void initLogCache(File cacheDir) {
        Message m = mLogFileHandler.obtainMessage(LogFileHandler.LOG_INIT, cacheDir);
        mLogFileHandler.sendMessage(m);

    }

    public static void flushLog() {
        mLogFileHandler.sendEmptyMessage(LogFileHandler.FLUSH_TO_DISK);
    }

    public enum ConnectionStatus {
        LEVEL_CONNECTED,
        LEVEL_VPNPAUSED,
        LEVEL_CONNECTING_SERVER_REPLIED,
        LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
        LEVEL_NONETWORK,
        LEVEL_NOTCONNECTED,
        LEVEL_START,
        LEVEL_AUTH_FAILED,
        LEVEL_WAITING_FOR_USER_INPUT,
        UNKNOWN_LEVEL
    }

    public enum LogLevel {
        INFO(2),
        ERROR(-2),
        WARNING(1),
        VERBOSE(3),
        DEBUG(4);

        protected int mValue;

        LogLevel(int value) {
            mValue = value;
        }

        public int getInt() {
            return mValue;
        }

        public static LogLevel getEnumByValue(int value) {
            switch (value) {
                case 1:
                    return INFO;
                case 2:
                    return ERROR;
                case 3:
                    return WARNING;
                case 4:
                    return DEBUG;
                default:
                    return null;
            }
        }
    }

    // keytool -printcert -jarfile de.blinkt.openvpn_85.apk
    public static final byte[] officalkey = {-58, -42, -44, -106, 90, -88, -87, -88, -52, -124, 84, 117, 66, 79, -112, -111, -46, 86, -37, 109};
    public static final byte[] officaldebugkey = {-99, -69, 45, 71, 114, -116, 82, 66, -99, -122, 50, -70, -56, -111, 98, -35, -65, 105, 82, 43};
    public static final byte[] amazonkey = {-116, -115, -118, -89, -116, -112, 120, 55, 79, -8, -119, -23, 106, -114, -85, -56, -4, 105, 26, -57};
    public static final byte[] fdroidkey = {-92, 111, -42, -46, 123, -96, -60, 79, -27, -31, 49, 103, 11, -54, -68, -27, 17, 2, 121, 104};


    private static ConnectionStatus mLastLevel = ConnectionStatus.LEVEL_NOTCONNECTED;

    private static final LogFileHandler mLogFileHandler;

    static {
        logbuffer = new LinkedList<>();
        logListener = new Vector<>();
        stateListener = new Vector<>();
        byteCountListener = new Vector<>();

        HandlerThread mHandlerThread = new HandlerThread("LogFileWriter", Thread.MIN_PRIORITY);
        mHandlerThread.start();
        mLogFileHandler = new LogFileHandler(mHandlerThread.getLooper());

        logInformation();

    }


    public static class LogItem implements Parcelable {
        private Object[] mArgs = null;
        private String mMessage = null;
        private int mRessourceId;
        // Default log priority
        LogLevel mLevel = LogLevel.INFO;
        private long logtime = System.currentTimeMillis();
        private int mVerbosityLevel = -1;

        private LogItem(int ressourceId, Object[] args) {
            mRessourceId = ressourceId;
            mArgs = args;
        }

        public LogItem(LogLevel level, int verblevel, String message) {
            mMessage = message;
            mLevel = level;
            mVerbosityLevel = verblevel;
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
            dest.writeInt(mLevel.getInt());
            dest.writeInt(mVerbosityLevel);

            dest.writeLong(logtime);
        }

        public LogItem(Parcel in) {
            mArgs = in.readArray(Object.class.getClassLoader());
            mMessage = in.readString();
            mRessourceId = in.readInt();
            mLevel = LogLevel.getEnumByValue(in.readInt());
            mVerbosityLevel = in.readInt();
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

        public LogItem(LogLevel loglevel, int ressourceId, Object... args) {
            mRessourceId = ressourceId;
            mArgs = args;
            mLevel = loglevel;
        }


        public LogItem(LogLevel loglevel, String msg) {
            mLevel = loglevel;
            mMessage = msg;
        }


        public LogItem(LogLevel loglevel, int ressourceId) {
            mRessourceId = ressourceId;
            mLevel = loglevel;
        }

        public String getString(Context c) {
            try {
                if (mMessage != null) {
                    return mMessage;
                } else {
                    if (c != null) {
                        if (mRessourceId == R.string.mobile_info)
                            return getMobileInfoString(c);
                        if (mArgs == null)
                            return c.getString(mRessourceId);
                        else
                            return c.getString(mRessourceId, mArgs);
                    } else {
                        String str = String.format(Locale.ENGLISH, "Log (no context) resid %d", mRessourceId);
                        if (mArgs != null)
                            str += TextUtils.join("|", mArgs);


                        return str;
                    }
                }
            } catch (UnknownFormatConversionException e) {
                if (c != null)
                    throw new UnknownFormatConversionException(e.getLocalizedMessage() + getString(null));
                else
                    throw e;
            } catch (java.util.FormatFlagsConversionMismatchException e) {
                if (c != null)
                    throw new FormatFlagsConversionMismatchException(e.getLocalizedMessage() + getString(null), e.getConversion());
                else
                    throw e;
            }

        }

        public LogLevel getLogLevel() {
            return mLevel;
        }

        // The lint is wrong here
        @SuppressLint("StringFormatMatches")
        private String getMobileInfoString(Context c) {
            c.getPackageManager();
            String apksign = "error getting package signature";

            String version = "error getting version";
            try {
                @SuppressLint("PackageManagerGetSignatures")
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
                else if (Arrays.equals(digest, fdroidkey))
                    apksign = "F-Droid built and signed version";
                else
                    apksign = c.getString(R.string.built_by, cert.getSubjectX500Principal().getName());

                PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
                version = packageinfo.versionName;

            } catch (NameNotFoundException | CertificateException |
                    NoSuchAlgorithmException ignored) {
            }

            Object[] argsext = Arrays.copyOf(mArgs, mArgs.length);
            argsext[argsext.length - 1] = apksign;
            argsext[argsext.length - 2] = version;

            return c.getString(R.string.mobile_info, argsext);

        }

        public long getLogtime() {
            return logtime;
        }


        public int getVerbosityLevel() {
            if (mVerbosityLevel == -1) {
                // Hack:
                // For message not from OpenVPN, report the status level as log level
                return mLevel.getInt();
            }
            return mVerbosityLevel;
        }
    }

    public interface LogListener {
        void newLog(LogItem logItem);
    }

    public interface StateListener {
        void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level);
    }

    public interface ByteCountListener {
        void updateByteCount(long in, long out, long diffIn, long diffOut);
    }

    public synchronized static void logMessage(LogLevel level, String prefix, String message) {
        newLogItem(new LogItem(level, prefix + message));

    }

    public synchronized static void clearLog() {
        logbuffer.clear();
        logInformation();
        mLogFileHandler.sendEmptyMessage(LogFileHandler.TRIM_LOG_FILE);
    }

    private static void logInformation() {
        logInfo(R.string.mobile_info, Build.MODEL, Build.BOARD, Build.BRAND, Build.VERSION.SDK_INT,
                NativeUtils.getNativeAPI(), Build.VERSION.RELEASE, Build.ID, Build.FINGERPRINT, "", "");
    }

    public synchronized static void addLogListener(LogListener ll) {
        logListener.add(ll);
    }

    public synchronized static void removeLogListener(LogListener ll) {
        logListener.remove(ll);
    }

    public synchronized static void addByteCountListener(ByteCountListener bcl) {
        bcl.updateByteCount(mlastByteCount[0], mlastByteCount[1], mlastByteCount[2], mlastByteCount[3]);
        byteCountListener.add(bcl);
    }

    public synchronized static void removeByteCountListener(ByteCountListener bcl) {
        byteCountListener.remove(bcl);
    }


    public synchronized static void addStateListener(StateListener sl) {
        if (!stateListener.contains(sl)) {
            stateListener.add(sl);
            if (mLaststate != null)
                sl.updateState(mLaststate, mLaststatemsg, mLastStateresid, mLastLevel);
        }
    }

    private static int getLocalizedState(String state) {
        switch (state) {
            case "CONNECTING":
                return R.string.state_connecting;
            case "WAIT":
                return R.string.state_wait;
            case "AUTH":
                return R.string.state_auth;
            case "GET_CONFIG":
                return R.string.state_get_config;
            case "ASSIGN_IP":
                return R.string.state_assign_ip;
            case "ADD_ROUTES":
                return R.string.state_add_routes;
            case "CONNECTED":
                return R.string.state_connected;
            case "DISCONNECTED":
                return R.string.state_disconnected;
            case "RECONNECTING":
                return R.string.state_reconnecting;
            case "EXITING":
                return R.string.state_exiting;
            case "RESOLVE":
                return R.string.state_resolve;
            case "TCP_CONNECT":
                return R.string.state_tcp_connect;
            default:
                return R.string.unknown_state;
        }

    }

    public static void updateStatePause(OpenVPNManagement.pauseReason pauseReason) {
        switch (pauseReason) {
            case noNetwork:
                VpnStatus.updateStateString("NONETWORK", "", R.string.state_nonetwork, ConnectionStatus.LEVEL_NONETWORK);
                break;
            case screenOff:
                VpnStatus.updateStateString("SCREENOFF", "", R.string.state_screenoff, ConnectionStatus.LEVEL_VPNPAUSED);
                break;
            case userPause:
                VpnStatus.updateStateString("USERPAUSE", "", R.string.state_userpause, ConnectionStatus.LEVEL_VPNPAUSED);
                break;
        }

    }

    private static ConnectionStatus getLevel(String state) {
        String[] noreplyet = {"CONNECTING", "WAIT", "RECONNECTING", "RESOLVE", "TCP_CONNECT"};
        String[] reply = {"AUTH", "GET_CONFIG", "ASSIGN_IP", "ADD_ROUTES"};
        String[] connected = {"CONNECTED"};
        String[] notconnected = {"DISCONNECTED", "EXITING"};

        for (String x : noreplyet)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;

        for (String x : reply)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;

        for (String x : connected)
            if (state.equals(x))
                return ConnectionStatus.LEVEL_CONNECTED;

        for (String x : notconnected)
            if (state.equals(x))
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

    public static void updateStateString(String state, String msg) {
        int rid = getLocalizedState(state);
        ConnectionStatus level = getLevel(state);
        updateStateString(state, msg, rid, level);
    }

    public synchronized static void updateStateString(String state, String msg, int resid, ConnectionStatus level) {
        // Workound for OpenVPN doing AUTH and wait and being connected
        // Simply ignore these state
        if (mLastLevel == ConnectionStatus.LEVEL_CONNECTED &&
                (state.equals("WAIT") || state.equals("AUTH"))) {
            newLogItem(new LogItem((LogLevel.DEBUG), String.format("Ignoring OpenVPN Status in CONNECTED state (%s->%s): %s", state, level.toString(), msg)));
            return;
        }

        mLaststate = state;
        mLaststatemsg = msg;
        mLastStateresid = resid;
        mLastLevel = level;


        for (StateListener sl : stateListener) {
            sl.updateState(state, msg, resid, level);
        }
        //newLogItem(new LogItem((LogLevel.DEBUG), String.format("New OpenVPN Status (%s->%s): %s",state,level.toString(),msg)));
    }

    public static void logInfo(String message) {
        newLogItem(new LogItem(LogLevel.INFO, message));
    }

    public static void logDebug(String message) {
        newLogItem(new LogItem(LogLevel.DEBUG, message));
    }

    public static void logInfo(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.INFO, resourceId, args));
    }

    public static void logDebug(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.DEBUG, resourceId, args));
    }

    private static void newLogItem(LogItem logItem) {
        newLogItem(logItem, false);
    }


    synchronized static void newLogItem(LogItem logItem, boolean cachedLine) {
        if (cachedLine) {
            logbuffer.addFirst(logItem);
        } else {
            logbuffer.addLast(logItem);
            Message m = mLogFileHandler.obtainMessage(LogFileHandler.LOG_MESSAGE, logItem);
            mLogFileHandler.sendMessage(m);
        }

        if (logbuffer.size() > MAXLOGENTRIES + MAXLOGENTRIES / 2) {
            while (logbuffer.size() > MAXLOGENTRIES)
                logbuffer.removeFirst();
            mLogFileHandler.sendMessage(mLogFileHandler.obtainMessage(LogFileHandler.TRIM_LOG_FILE));
        }


        for (LogListener ll : logListener) {
            ll.newLog(logItem);
        }
    }


    public static void logError(String msg) {
        newLogItem(new LogItem(LogLevel.ERROR, msg));

    }

    public static void logWarning(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.WARNING, resourceId, args));
    }

    public static void logWarning(String msg) {
        newLogItem(new LogItem(LogLevel.WARNING, msg));
    }


    public static void logError(int resourceId) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId));
    }

    public static void logError(int resourceId, Object... args) {
        newLogItem(new LogItem(LogLevel.ERROR, resourceId, args));
    }

    public static void logMessageOpenVPN(LogLevel level, int ovpnlevel, String message) {
        newLogItem(new LogItem(level, ovpnlevel, message));

    }


    public static synchronized void updateByteCount(long in, long out) {
        long lastIn = mlastByteCount[0];
        long lastOut = mlastByteCount[1];
        long diffIn = mlastByteCount[2] = in - lastIn;
        long diffOut = mlastByteCount[3] = out - lastOut;


        mlastByteCount = new long[]{in, out, diffIn, diffOut};
        for (ByteCountListener bcl : byteCountListener) {
            bcl.updateByteCount(in, out, diffIn, diffOut);
        }
    }


}
