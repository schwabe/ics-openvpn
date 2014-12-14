/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import de.blinkt.openvpn.BuildConfig;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.activities.LogWindow;
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;
import de.blinkt.openvpn.core.VpnStatus.ConnectionStatus;
import de.blinkt.openvpn.core.VpnStatus.StateListener;

import static de.blinkt.openvpn.core.NetworkSpace.ipAddress;
import static de.blinkt.openvpn.core.VpnStatus.ConnectionStatus.LEVEL_CONNECTED;
import static de.blinkt.openvpn.core.VpnStatus.ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
import static de.blinkt.openvpn.core.VpnStatus.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;

public class OpenVPNService extends VpnService implements StateListener, Callback, ByteCountListener {
    public static final String START_SERVICE = "de.blinkt.openvpn.START_SERVICE";
    public static final String START_SERVICE_STICKY = "de.blinkt.openvpn.START_SERVICE_STICKY";
    public static final String ALWAYS_SHOW_NOTIFICATION = "de.blinkt.openvpn.NOTIFICATION_ALWAYS_VISIBLE";
    public static final String DISCONNECT_VPN = "de.blinkt.openvpn.DISCONNECT_VPN";
    private static final String PAUSE_VPN = "de.blinkt.openvpn.PAUSE_VPN";
    private static final String RESUME_VPN = "de.blinkt.openvpn.RESUME_VPN";
    private static final int OPENVPN_STATUS = 1;
    private static boolean mNotificationAlwaysVisible = false;
    private final Vector<String> mDnslist = new Vector<String>();
    private final NetworkSpace mRoutes = new NetworkSpace();
    private final NetworkSpace mRoutesv6 = new NetworkSpace();
    private final IBinder mBinder = new LocalBinder();
    private Thread mProcessThread = null;
    private VpnProfile mProfile;
    private String mDomain = null;
    private CIDRIP mLocalIP = null;
    private int mMtu;
    private String mLocalIPv6 = null;
    private DeviceStateReceiver mDeviceStateReceiver;
    private boolean mDisplayBytecount = false;
    private boolean mStarting = false;
    private long mConnecttime;
    private boolean mOvpn3 = false;
    private OpenVPNManagement mManagement;
    private String mLastTunCfg;
    private String mRemoteGW;
    private final Object mProcessLock = new Object();
    private LollipopDeviceStateListener mLollipopDeviceStateListener;

    // From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean mbit) {
        if (mbit)
            bytes = bytes * 8;
        int unit = mbit ? 1000 : 1024;
        if (bytes < unit)
            return bytes + (mbit ? " bit" : " B");

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (mbit ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (mbit ? "" : "");
        if (mbit)
            return String.format(Locale.getDefault(), "%.1f %sbit", bytes / Math.pow(unit, exp), pre);
        else
            return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public IBinder onBind(Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(START_SERVICE))
            return mBinder;
        else
            return super.onBind(intent);
    }

    @Override
    public void onRevoke() {
        mManagement.stopVPN();
        endVpnService();
    }

    // Similar to revoke but do not try to stop process
    public void processDied() {
        endVpnService();
    }

    private void endVpnService() {
        synchronized (mProcessLock) {
            mProcessThread = null;
        }
        VpnStatus.removeByteCountListener(this);
        unregisterDeviceStateReceiver();
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        if (!mStarting) {
            stopForeground(!mNotificationAlwaysVisible);

            if (!mNotificationAlwaysVisible) {
                stopSelf();
                VpnStatus.removeStateListener(this);
            }
        }
    }

    private void showNotification(String msg, String tickerText, boolean lowpriority, long when, ConnectionStatus status) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);


        int icon = getIconByConnectionStatus(status);

        android.app.Notification.Builder nbuilder = new Notification.Builder(this);

        if (mProfile != null)
            nbuilder.setContentTitle(getString(R.string.notifcation_title, mProfile.mName));
        else
            nbuilder.setContentTitle(getString(R.string.notifcation_title_notconnect));

        nbuilder.setContentText(msg);
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setOngoing(true);
        nbuilder.setContentIntent(getLogPendingIntent());
        nbuilder.setSmallIcon(icon);


        if (when != 0)
            nbuilder.setWhen(when);


        // Try to set the priority available since API 16 (Jellybean)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            jbNotificationExtras(lowpriority, nbuilder);

        if (tickerText != null && !tickerText.equals(""))
            nbuilder.setTicker(tickerText);

        @SuppressWarnings("deprecation")
        Notification notification = nbuilder.getNotification();


        mNotificationManager.notify(OPENVPN_STATUS, notification);
        startForeground(OPENVPN_STATUS, notification);
    }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_vpn;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_empty_halo;
            case LEVEL_VPNPAUSED:
                return android.R.drawable.ic_media_pause;
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_vpn;

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void jbNotificationExtras(boolean lowpriority,
                                      android.app.Notification.Builder nbuilder) {
        try {
            if (lowpriority) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                // PRIORITY_MIN == -2
                setpriority.invoke(nbuilder, -2);

                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);

            }

            Intent disconnectVPN = new Intent(this, DisconnectVPN.class);
            disconnectVPN.setAction(DISCONNECT_VPN);
            PendingIntent disconnectPendingIntent = PendingIntent.getActivity(this, 0, disconnectVPN, 0);

            nbuilder.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                    getString(R.string.cancel_connection), disconnectPendingIntent);

            Intent pauseVPN = new Intent(this, OpenVPNService.class);
            if (mDeviceStateReceiver == null || !mDeviceStateReceiver.isUserPaused()) {
                pauseVPN.setAction(PAUSE_VPN);
                PendingIntent pauseVPNPending = PendingIntent.getService(this, 0, pauseVPN, 0);
                nbuilder.addAction(android.R.drawable.ic_media_pause,
                        getString(R.string.pauseVPN), pauseVPNPending);

            } else {
                pauseVPN.setAction(RESUME_VPN);
                PendingIntent resumeVPNPending = PendingIntent.getService(this, 0, pauseVPN, 0);
                nbuilder.addAction(android.R.drawable.ic_media_play,
                        getString(R.string.resumevpn), resumeVPNPending);
            }


            //ignore exception
        } catch (NoSuchMethodException nsm) {
            VpnStatus.logException(nsm);
        } catch (IllegalArgumentException e) {
            VpnStatus.logException(e);
        } catch (IllegalAccessException e) {
            VpnStatus.logException(e);
        } catch (InvocationTargetException e) {
            VpnStatus.logException(e);
        }

    }

    PendingIntent getLogPendingIntent() {
        // Let the configure Button show the Log
        Intent intent = new Intent(getBaseContext(), LogWindow.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startLW = PendingIntent.getActivity(this, 0, intent, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;

    }

    synchronized void registerDeviceStateReceiver(OpenVPNManagement magnagement) {
        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mDeviceStateReceiver = new DeviceStateReceiver(magnagement);
        registerReceiver(mDeviceStateReceiver, filter);
        VpnStatus.addByteCountListener(mDeviceStateReceiver);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            addLollipopCMListener(); */
    }

    synchronized void unregisterDeviceStateReceiver() {
        if (mDeviceStateReceiver != null)
            try {
                VpnStatus.removeByteCountListener(mDeviceStateReceiver);
                this.unregisterReceiver(mDeviceStateReceiver);
            } catch (IllegalArgumentException iae) {
                // I don't know why  this happens:
                // java.lang.IllegalArgumentException: Receiver not registered: de.blinkt.openvpn.NetworkSateReceiver@41a61a10
                // Ignore for now ...
                iae.printStackTrace();
            }
        mDeviceStateReceiver = null;

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            removeLollipopCMListener();*/

    }

    public void userPause(boolean shouldBePaused) {
        if (mDeviceStateReceiver != null)
            mDeviceStateReceiver.userPause(shouldBePaused);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getBooleanExtra(ALWAYS_SHOW_NOTIFICATION, false))
            mNotificationAlwaysVisible = true;

        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);

        if (intent != null && PAUSE_VPN.equals(intent.getAction())) {
            if (mDeviceStateReceiver != null)
                mDeviceStateReceiver.userPause(true);
            return START_NOT_STICKY;
        }

        if (intent != null && RESUME_VPN.equals(intent.getAction())) {
            if (mDeviceStateReceiver != null)
                mDeviceStateReceiver.userPause(false);
            return START_NOT_STICKY;
        }


        if (intent != null && START_SERVICE.equals(intent.getAction()))
            return START_NOT_STICKY;
        if (intent != null && START_SERVICE_STICKY.equals(intent.getAction())) {
            return START_REDELIVER_INTENT;
        }

        /* The intent is null when the service has been restarted */
        if (intent == null) {
            mProfile = ProfileManager.getLastConnectedProfile(this, false);

            /* Got no profile, just stop */
            if (mProfile == null) {
                Log.d("OpenVPN", "Got no last connected profile on null intent. Stopping");
                stopSelf(startId);
                return START_NOT_STICKY;
            }
            /* Do the asynchronous keychain certificate stuff */
            mProfile.checkForRestart(this);

            /* Recreate the intent */
            intent = mProfile.getStartServiceIntent(this);

        } else {
            String profileUUID = intent.getStringExtra(getPackageName() + ".profileUUID");
            mProfile = ProfileManager.get(this, profileUUID);
        }


        // Extract information from the intent.
        String prefix = getPackageName();
        String[] argv = intent.getStringArrayExtra(prefix + ".ARGV");
        String nativeLibraryDirectory = intent.getStringExtra(prefix + ".nativelib");

        String startTitle = getString(R.string.start_vpn_title, mProfile.mName);
        String startTicker = getString(R.string.start_vpn_ticker, mProfile.mName);
        showNotification(startTitle, startTicker,
                false, 0, LEVEL_CONNECTING_NO_SERVER_REPLY_YET);

        // Set a flag that we are starting a new VPN
        mStarting = true;
        // Stop the previous session by interrupting the thread.
        if (mManagement != null && mManagement.stopVPN())
            // an old was asked to exit, wait 1s
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }

        synchronized (mProcessLock) {
            if (mProcessThread != null) {
                mProcessThread.interrupt();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
        // An old running VPN should now be exited
        mStarting = false;

        // Start a new session by creating a new thread.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        mOvpn3 = prefs.getBoolean("ovpn3", false);
        if (!"ovpn3".equals(BuildConfig.FLAVOR))
            mOvpn3 = false;


        // Open the Management Interface
        if (!mOvpn3) {

            // start a Thread that handles incoming messages of the managment socket
            OpenVpnManagementThread ovpnManagementThread = new OpenVpnManagementThread(mProfile, this);
            if (ovpnManagementThread.openManagementInterface(this)) {

                Thread mSocketManagerThread = new Thread(ovpnManagementThread, "OpenVPNManagementThread");
                mSocketManagerThread.start();
                mManagement = ovpnManagementThread;
                VpnStatus.logInfo("started Socket Thread");
            } else {
                return START_NOT_STICKY;
            }
        }


        Runnable processThread;
        if (mOvpn3) {

            OpenVPNManagement mOpenVPN3 = instantiateOpenVPN3Core();
            processThread = (Runnable) mOpenVPN3;
            mManagement = mOpenVPN3;


        } else {
            HashMap<String, String> env = new HashMap<String, String>();
            processThread = new OpenVPNThread(this, argv, env, nativeLibraryDirectory);
        }

        synchronized (mProcessLock) {
            mProcessThread = new Thread(processThread, "OpenVPNProcessThread");
            mProcessThread.start();
        }
        if (mDeviceStateReceiver != null)
            unregisterDeviceStateReceiver();

        registerDeviceStateReceiver(mManagement);


        ProfileManager.setConnectedVpnProfile(this, mProfile);
        /* TODO: At the moment we have no way to handle asynchronous PW input
         * Fixing will also allow to handle challenge/responsee authentication */
        if (mProfile.needUserPWInput(true) != 0)
            return START_NOT_STICKY;

        return START_STICKY;
    }

    private OpenVPNManagement instantiateOpenVPN3Core() {
        try {
            Class cl = Class.forName("de.blinkt.openvpn.core.OpenVPNThreadv3");
            return (OpenVPNManagement) cl.getConstructor(OpenVPNService.class, VpnProfile.class).newInstance(this, mProfile);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onDestroy() {
        synchronized (mProcessLock) {
            if (mProcessThread != null) {
                mManagement.stopVPN();
            }
        }

        if (mDeviceStateReceiver != null) {
            this.unregisterReceiver(mDeviceStateReceiver);
        }
        // Just in case unregister for state
        VpnStatus.removeStateListener(this);

    }

    private String getTunConfigString() {
        // The format of the string is not important, only that
        // two identical configurations produce the same result
        String cfg = "TUNCFG UNQIUE STRING ips:";

        if (mLocalIP != null)
            cfg += mLocalIP.toString();
        if (mLocalIPv6 != null)
            cfg += mLocalIPv6;


        cfg += "routes: " + TextUtils.join("|", mRoutes.getNetworks(true)) + TextUtils.join("|", mRoutesv6.getNetworks(true));
        cfg += "excl. routes:" + TextUtils.join("|", mRoutes.getNetworks(false)) + TextUtils.join("|", mRoutesv6.getNetworks(false));
        cfg += "dns: " + TextUtils.join("|", mDnslist);
        cfg += "domain: " + mDomain;
        cfg += "mtu: " + mMtu;
        return cfg;
    }

    public ParcelFileDescriptor openTun() {

        //Debug.startMethodTracing(getExternalFilesDir(null).toString() + "/opentun.trace", 40* 1024 * 1024);

        Builder builder = new Builder();

        VpnStatus.logInfo(R.string.last_openvpn_tun_config);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mProfile.mAllowLocalLAN)
        {
            allowAllAFFamilies(builder);
        }

        if (mLocalIP == null && mLocalIPv6 == null) {
            VpnStatus.logError(getString(R.string.opentun_no_ipaddr));
            return null;
        }

        if (mLocalIP != null) {
            addLocalNetworksToRoutes();
            try {
                builder.addAddress(mLocalIP.mIp, mLocalIP.len);
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.dns_add_error, mLocalIP, iae.getLocalizedMessage());
                return null;
            }
        }

        if (mLocalIPv6 != null) {
            String[] ipv6parts = mLocalIPv6.split("/");
            try {
                builder.addAddress(ipv6parts[0], Integer.parseInt(ipv6parts[1]));
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.ip_add_error, mLocalIPv6, iae.getLocalizedMessage());
                return null;
            }

        }


        for (String dns : mDnslist) {
            try {
                builder.addDnsServer(dns);
            } catch (IllegalArgumentException iae) {
                VpnStatus.logError(R.string.dns_add_error, dns, iae.getLocalizedMessage());
            }
        }

        String release = Build.VERSION.RELEASE;
        if ((Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                && mMtu < 1280) {
            VpnStatus.logInfo(String.format(Locale.US, "Forcing MTU to 1280 instead of %d to workaround Android Bug #70916", mMtu));
            builder.setMtu(1280);
        } else {
            builder.setMtu(mMtu);
        }

        Collection<ipAddress> positiveIPv4Routes = mRoutes.getPositiveIPList();
        Collection<ipAddress> positiveIPv6Routes = mRoutesv6.getPositiveIPList();

        for (NetworkSpace.ipAddress route : positiveIPv4Routes) {
            try {
                builder.addRoute(route.getIPv4Address(), route.networkMask);
            } catch (IllegalArgumentException ia) {
                VpnStatus.logError(getString(R.string.route_rejected) + route + " " + ia.getLocalizedMessage());
            }
        }

        for (NetworkSpace.ipAddress route6 : positiveIPv6Routes) {
            try {
                builder.addRoute(route6.getIPv6Address(), route6.networkMask);
            } catch (IllegalArgumentException ia) {
                VpnStatus.logError(getString(R.string.route_rejected) + route6 + " " + ia.getLocalizedMessage());
            }
        }

        if (mDomain != null)
            builder.addSearchDomain(mDomain);

        VpnStatus.logInfo(R.string.local_ip_info, mLocalIP.mIp, mLocalIP.len, mLocalIPv6, mMtu);
        VpnStatus.logInfo(R.string.dns_server_info, TextUtils.join(", ", mDnslist), mDomain);
        VpnStatus.logInfo(R.string.routes_info_incl, TextUtils.join(", ", mRoutes.getNetworks(true)), TextUtils.join(", ", mRoutesv6.getNetworks(true)));
        VpnStatus.logInfo(R.string.routes_info_excl, TextUtils.join(", ", mRoutes.getNetworks(false)), TextUtils.join(", ", mRoutesv6.getNetworks(false)));
        VpnStatus.logDebug(R.string.routes_debug, TextUtils.join(", ", positiveIPv4Routes), TextUtils.join(", ", positiveIPv6Routes));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setAllowedVpnPackages(builder);
        }


        String session = mProfile.mName;
        if (mLocalIP != null && mLocalIPv6 != null)
            session = getString(R.string.session_ipv6string, session, mLocalIP, mLocalIPv6);
        else if (mLocalIP != null)
            session = getString(R.string.session_ipv4string, session, mLocalIP);

        builder.setSession(session);

        // No DNS Server, log a warning
        if (mDnslist.size() == 0)
            VpnStatus.logInfo(R.string.warn_no_dns);

        mLastTunCfg = getTunConfigString();

        // Reset information
        mDnslist.clear();
        mRoutes.clear();
        mRoutesv6.clear();
        mLocalIP = null;
        mLocalIPv6 = null;
        mDomain = null;

        builder.setConfigureIntent(getLogPendingIntent());

        try {
            //Debug.stopMethodTracing();
            return builder.establish();
        } catch (Exception e) {
            VpnStatus.logError(R.string.tun_open_error);
            VpnStatus.logError(getString(R.string.error) + e.getLocalizedMessage());
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                VpnStatus.logError(R.string.tun_error_helpful);
            }
            return null;
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void allowAllAFFamilies(Builder builder) {
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void removeLollipopCMListener() {
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(CONNECTIVITY_SERVICE);
        cm.unregisterNetworkCallback(mLollipopDeviceStateListener);
        mLollipopDeviceStateListener = null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    void addLollipopCMListener() {
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(CONNECTIVITY_SERVICE);
        NetworkRequest.Builder nrb = new NetworkRequest.Builder();

        mLollipopDeviceStateListener = new LollipopDeviceStateListener();
        cm.registerNetworkCallback(nrb.build(), mLollipopDeviceStateListener);
    }

    private void addLocalNetworksToRoutes() {

        // Add local network interfaces
        String[] localRoutes = NativeUtils.getIfconfig();

        // The format of mLocalRoutes is kind of broken because I don't really like JNI
        for (int i = 0; i < localRoutes.length; i += 3) {
            String intf = localRoutes[i];
            String ipAddr = localRoutes[i + 1];
            String netMask = localRoutes[i + 2];

            if (intf == null || intf.equals("lo") ||
                    intf.startsWith("tun") || intf.startsWith("rmnet"))
                continue;

            if (ipAddr==null || netMask == null) {
                VpnStatus.logError("Local routes are broken?! (Report to author) " + TextUtils.join("|", localRoutes));
                continue;
            }
            
            if (ipAddr.equals(mLocalIP.mIp))
                continue;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT && !mProfile.mAllowLocalLAN) {
                mRoutes.addIPSplit(new CIDRIP(ipAddr, netMask), true);

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mProfile.mAllowLocalLAN)
                mRoutes.addIP(new CIDRIP(ipAddr, netMask), false);
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setAllowedVpnPackages(Builder builder) {
        for (String pkg : mProfile.mAllowedAppsVpn) {
            try {
                if (mProfile.mAllowedAppsVpnAreDisallowed) {
                    builder.addDisallowedApplication(pkg);
                } else {
                    builder.addAllowedApplication(pkg);
                }
            } catch (PackageManager.NameNotFoundException e) {
                mProfile.mAllowedAppsVpn.remove(pkg);
                VpnStatus.logInfo(R.string.app_no_longer_exists, pkg);
            }
        }

        if (mProfile.mAllowedAppsVpnAreDisallowed) {
            VpnStatus.logDebug(R.string.disallowed_vpn_apps_info, TextUtils.join(", ", mProfile.mAllowedAppsVpn));
        } else {
            VpnStatus.logDebug(R.string.allowed_vpn_apps_info, TextUtils.join(", ", mProfile.mAllowedAppsVpn));
        }
    }

    public void addDNS(String dns) {
        mDnslist.add(dns);
    }

    public void setDomain(String domain) {
        if (mDomain == null) {
            mDomain = domain;
        }
    }

    /**
     * Route that is always included, used by the v3 core
     */
    public void addRoute(CIDRIP route) {
        mRoutes.addIP(route, true);
    }

    public void addRoute(String dest, String mask, String gateway, String device) {
        CIDRIP route = new CIDRIP(dest, mask);
        boolean include = isAndroidTunDevice(device);

        NetworkSpace.ipAddress gatewayIP = new NetworkSpace.ipAddress(new CIDRIP(gateway, 32), false);

        if (mLocalIP == null) {
            VpnStatus.logError("Local IP address unset but adding route?! This is broken! Please contact author with log");
            return;
        }
        NetworkSpace.ipAddress localNet = new NetworkSpace.ipAddress(mLocalIP, true);
        if (localNet.containsNet(gatewayIP))
            include = true;

        if (gateway != null &&
                (gateway.equals("255.255.255.255") || gateway.equals(mRemoteGW)))
            include = true;


        if (route.len == 32 && !mask.equals("255.255.255.255")) {
            VpnStatus.logWarning(R.string.route_not_cidr, dest, mask);
        }

        if (route.normalise())
            VpnStatus.logWarning(R.string.route_not_netip, dest, route.len, route.mIp);

        mRoutes.addIP(route, include);
    }

    public void addRoutev6(String network, String device) {
        String[] v6parts = network.split("/");
        boolean included = isAndroidTunDevice(device);

        // Tun is opened after ROUTE6, no device name may be present

        try {
            Inet6Address ip = (Inet6Address) InetAddress.getAllByName(v6parts[0])[0];
            int mask = Integer.parseInt(v6parts[1]);
            mRoutesv6.addIPv6(ip, mask, included);

        } catch (UnknownHostException e) {
            VpnStatus.logException(e);
        }


    }

    private boolean isAndroidTunDevice(String device) {
        return device != null &&
                (device.startsWith("tun") || "(null)".equals(device) || "vpnservice-tun".equals(device));
    }

    public void setMtu(int mtu) {
        mMtu = mtu;
    }

    public void setLocalIP(CIDRIP cdrip) {
        mLocalIP = cdrip;
    }

    public void setLocalIP(String local, String netmask, int mtu, String mode) {
        mLocalIP = new CIDRIP(local, netmask);
        mMtu = mtu;
        mRemoteGW = null;

        long netMaskAsInt = CIDRIP.getInt(netmask);

        if (mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
            // get the netmask as IP

            int masklen;
            long mask;
            if ("net30".equals(mode)) {
                masklen = 30;
                mask = 0xfffffffc;
            } else {
                masklen = 31;
                mask = 0xfffffffe;
            }

            // Netmask is Ip address +/-1, assume net30/p2p with small net
            if ((netMaskAsInt & mask) == (mLocalIP.getInt() & mask)) {
                mLocalIP.len = masklen;
            } else {
                mLocalIP.len = 32;
                if (!"p2p".equals(mode))
                    VpnStatus.logWarning(R.string.ip_not_cidr, local, netmask, mode);
            }
        }
        if (("p2p".equals(mode) && mLocalIP.len < 32) || ("net30".equals(mode) && mLocalIP.len < 30)) {
            VpnStatus.logWarning(R.string.ip_looks_like_subnet, local, netmask, mode);
        }


        /* Workaround for Lollipop, it  does not route traffic to the VPNs own network mask */
        if (mLocalIP.len <= 31 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            addRoute(mLocalIP);


        // Configurations are sometimes really broken...
        mRemoteGW = netmask;
    }

    public void setLocalIPv6(String ipv6addr) {
        mLocalIPv6 = ipv6addr;
    }

    @Override
    public void updateState(String state, String logmessage, int resid, ConnectionStatus level) {
        // If the process is not running, ignore any state,
        // Notification should be invisible in this state

        doSendBroadcast(state, level);
        if (mProcessThread == null && !mNotificationAlwaysVisible)
            return;

        boolean lowpriority = false;
        // Display byte count only after being connected

        {
            if (level == LEVEL_WAITING_FOR_USER_INPUT) {
                // The user is presented a dialog of some kind, no need to inform the user
                // with a notifcation
                return;
            } else if (level == LEVEL_CONNECTED) {
                mDisplayBytecount = true;
                mConnecttime = System.currentTimeMillis();
                lowpriority = true;
            } else {
                mDisplayBytecount = false;
            }

            // Other notifications are shown,
            // This also mean we are no longer connected, ignore bytecount messages until next
            // CONNECTED
            // Does not work :(
            String msg = getString(resid);
            String ticker = msg;
            showNotification(msg + " " + logmessage, ticker, lowpriority, 0, level);

        }
    }

    private void doSendBroadcast(String state, ConnectionStatus level) {
        Intent vpnstatus = new Intent();
        vpnstatus.setAction("de.blinkt.openvpn.VPN_STATUS");
        vpnstatus.putExtra("status", level.toString());
        vpnstatus.putExtra("detailstatus", state);
        sendBroadcast(vpnstatus, permission.ACCESS_NETWORK_STATE);
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (mDisplayBytecount) {
            String netstat = String.format(getString(R.string.statusline_bytecount),
                    humanReadableByteCount(in, false),
                    humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true),
                    humanReadableByteCount(out, false),
                    humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true));

            boolean lowpriority = !mNotificationAlwaysVisible;
            showNotification(netstat, null, lowpriority, mConnecttime, LEVEL_CONNECTED);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {
        Runnable r = msg.getCallback();
        if (r != null) {
            r.run();
            return true;
        } else {
            return false;
        }
    }

    public OpenVPNManagement getManagement() {
        return mManagement;
    }

    public String getTunReopenStatus() {
        String currentConfiguration = getTunConfigString();
        if (currentConfiguration.equals(mLastTunCfg)) {
            return "NOACTION";
        } else {
            String release = Build.VERSION.RELEASE;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                // There will be probably no 4.4.4 or 4.4.5 version, so don't waste effort to do parsing here
                return "OPEN_AFTER_CLOSE";
            else
                return "OPEN_BEFORE_CLOSE";
        }
    }

    public class LocalBinder extends Binder {
        public OpenVPNService getService() {
            // Return this instance of LocalService so clients can call public methods
            return OpenVPNService.this;
        }
    }
}
