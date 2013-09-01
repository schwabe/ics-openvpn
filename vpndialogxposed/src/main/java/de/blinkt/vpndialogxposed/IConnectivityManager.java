/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /Users/arne/software/icsopenvpn/vpndialogxposed/src/main/aidl/android/net/IConnectivityManager.aidl
 */
package de.blinkt.vpndialogxposed;
/**
 * Interface that answers queries about, and allows changing, the
 * state of network connectivity.
 */

/**
 * {@hide}
 */
public interface IConnectivityManager extends android.os.IInterface {
    public boolean prepareVpn(java.lang.String oldPackage, java.lang.String newPackage) throws android.os.RemoteException;

    /** Local-side IPC implementation stub class. */
    public static abstract class Stub extends android.os.Binder implements IConnectivityManager {
        static final int TRANSACTION_prepareVpn = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        private static final java.lang.String DESCRIPTOR = "android.net.IConnectivityManager";

        /** Construct the stub at attach it to the interface. */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        /**
         * Cast an IBinder object into an android.net.IConnectivityManager interface,
         * generating a proxy if needed.
         */
        public static IConnectivityManager asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof IConnectivityManager))) {
                return ((IConnectivityManager) iin);
            }
            return new IConnectivityManager.Stub.Proxy(obj);
        }

        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_prepareVpn: {
                    data.enforceInterface(DESCRIPTOR);
                    java.lang.String _arg0;
                    _arg0 = data.readString();
                    java.lang.String _arg1;
                    _arg1 = data.readString();
                    boolean _result = this.prepareVpn(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(((_result) ? (1) : (0)));
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IConnectivityManager {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public java.lang.String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public boolean prepareVpn(java.lang.String oldPackage, java.lang.String newPackage) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(oldPackage);
                    _data.writeString(newPackage);
                    mRemote.transact(Stub.TRANSACTION_prepareVpn, _data, _reply, 0);
                    _reply.readException();
                    _result = (0 != _reply.readInt());
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}
