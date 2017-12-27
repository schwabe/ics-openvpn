/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.api;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNService;

public class RemoteAction extends Activity {

        private ExternalAppDatabase mExtAppDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExtAppDb = new ExternalAppDatabase(this);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            IOpenVPNServiceInternal myservice = IOpenVPNServiceInternal.Stub.asInterface(service);
            try {
                myservice.stopVPN(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            //mService = null;
        }

    };


    @Override
    protected void onResume() {
        super.onResume();
        if (mExtAppDb.checkRemoteActionPermission(this))
            performAction();

        finish();

    }

    private void performAction() {
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);

        ComponentName component = getIntent().getComponent();
        if (component.getShortClassName().equals(".api.DisconnectVPN")) {
            boolean mDoDisconnect = true;
        }
        getApplicationContext().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);


    }
}
