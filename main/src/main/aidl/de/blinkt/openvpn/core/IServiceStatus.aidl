// StatusIPC.aidl
package de.blinkt.openvpn.core;

// Declare any non-default types here with import statements
import de.blinkt.openvpn.core.IStatusCallbacks;
import android.os.ParcelFileDescriptor;

interface IServiceStatus {
         /**
          * Registers to receive OpenVPN Status Updates and gets a
          * ParcelFileDescript back that contains the log up to that point
          */
         ParcelFileDescriptor registerStatusCallback(in IStatusCallbacks cb);

         /**
           * Remove a previously registered callback interface.
           */
        void unregisterStatusCallback(in IStatusCallbacks cb);

        /**
         * Returns the last connedcted VPN
         */
        String getLastConnectedVPN();

        /**
          * Sets a cached password
          */
       void setCachedPassword(in String uuid, int type, String password);
}
