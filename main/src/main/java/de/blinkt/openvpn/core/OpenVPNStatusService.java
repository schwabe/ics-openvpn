/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.lang.ref.WeakReference;

import de.blinkt.openvpn.api.ExternalOpenVPNService;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback;

/**
 * Created by arne on 08.11.16.
 */

public class OpenVPNStatusService extends Service implements VpnStatus.LogListener, VpnStatus.ByteCountListener, VpnStatus.StateListener {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    static final RemoteCallbackList<IStatusCallbacks> mCallbacks =
            new RemoteCallbackList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        VpnStatus.addLogListener(this);
        VpnStatus.addByteCountListener(this);
        VpnStatus.addStateListener(this);
        mHandler.setService(this);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        VpnStatus.removeLogListener(this);
        VpnStatus.removeByteCountListener(this);
        VpnStatus.removeStateListener(this);
        mCallbacks.kill();

    }

    private static final IServiceStatus.Stub mBinder = new IServiceStatus.Stub() {

        @Override
        public void registerStatusCallback(IStatusCallbacks cb) throws RemoteException {
            mCallbacks.register(cb);
        }

        @Override
        public void unregisterStatusCallback(IStatusCallbacks cb) throws RemoteException {
            mCallbacks.unregister(cb);
        }

        @Override
        public String getLastConnectedVPN() throws RemoteException {
            return VpnStatus.getLastConnectedVPNProfile();
        }
    };

    @Override
    public void newLog(LogItem logItem) {
        Message msg = mHandler.obtainMessage(SEND_NEW_LOGITEM, logItem);
        msg.sendToTarget();
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        Message msg = mHandler.obtainMessage(SEND_NEW_BYTECOUNT, Pair.create(in, out));
        msg.sendToTarget();
    }

    class UpdateMessage {
        public String state;
        public String logmessage;
        public ConnectionStatus level;
        public int resId;

        public UpdateMessage(String state, String logmessage, int resId, ConnectionStatus level) {
            this.state = state;
            this.resId = resId;
            this.logmessage = logmessage;
            this.level = level;
        }
    }


    @Override
    public void updateState(String state, String logmessage, int localizedResId, ConnectionStatus level) {

        Message msg = mHandler.obtainMessage(SEND_NEW_STATE, new UpdateMessage(state, logmessage, localizedResId, level));
        msg.sendToTarget();
    }

    @Override
    public void setConnectedVPN(String uuid) {
        Message msg = mHandler.obtainMessage(SEND_NEW_CONNECTED_VPN, uuid);
        msg.sendToTarget();
    }

    private static final OpenVPNStatusHandler mHandler = new OpenVPNStatusHandler();

    private static final int SEND_NEW_LOGITEM = 100;
    private static final int SEND_NEW_STATE = 101;
    private static final int SEND_NEW_BYTECOUNT = 102;
    private static final int SEND_NEW_CONNECTED_VPN = 103;

    static class OpenVPNStatusHandler extends Handler {
        WeakReference<OpenVPNStatusService> service = null;

        private void setService(OpenVPNStatusService statusService) {
            service = new WeakReference<>(statusService);
        }

        @Override
        public void handleMessage(Message msg) {

            RemoteCallbackList<IStatusCallbacks> callbacks;
            if (service == null || service.get() == null)
                return;
            callbacks = service.get().mCallbacks;
            // Broadcast to all clients the new value.
            final int N = callbacks.beginBroadcast();
            for (int i = 0; i < N; i++) {

                try {
                    IStatusCallbacks broadcastItem = callbacks.getBroadcastItem(i);

                    switch (msg.what) {
                        case SEND_NEW_LOGITEM:
                            broadcastItem.newLogItem((LogItem) msg.obj);
                            break;
                        case SEND_NEW_BYTECOUNT:
                            Pair<Long, Long> inout = (Pair<Long, Long>) msg.obj;
                            broadcastItem.updateByteCount(inout.first, inout.second);
                            break;
                        case SEND_NEW_STATE:
                            sendUpdate(broadcastItem, (UpdateMessage) msg.obj);
                            break;

                        case SEND_NEW_CONNECTED_VPN:
                            broadcastItem.connectedVPN((String) msg.obj);
                            break;
                    }
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            callbacks.finishBroadcast();
        }
    }

    private static void sendUpdate(IStatusCallbacks broadcastItem,
                                   UpdateMessage um) throws RemoteException {
        broadcastItem.updateStateString(um.state, um.logmessage, um.resId, um.level);
    }
}