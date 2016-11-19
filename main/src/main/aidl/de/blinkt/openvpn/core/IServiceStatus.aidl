// StatusIPC.aidl
package de.blinkt.openvpn.core;

// Declare any non-default types here with import statements
import de.blinkt.openvpn.core.IStatusCallbacks;


interface IServiceStatus {
         /**
          * Registers to receive OpenVPN Status Updates
          */
         void registerStatusCallback(in IStatusCallbacks cb);

         /**
           * Remove a previously registered callback interface.
           */
        void unregisterStatusCallback(in IStatusCallbacks cb);

        /**
         *
         */
        String getLastConnectedVPN();
}
