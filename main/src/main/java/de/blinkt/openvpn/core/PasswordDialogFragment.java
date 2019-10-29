/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputType;
import android.widget.EditText;


public class PasswordDialogFragment extends DialogFragment {

    private IOpenVPNServiceInternal mService;
    private ServiceConnection mConnection = new ServiceConnection() {


        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(getActivity(), OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        getActivity().bindService(intent, mConnection, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mConnection);
    }

    static PasswordDialogFragment newInstance(String title) {
        PasswordDialogFragment frag = new PasswordDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);


        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    if (mService != null) {
                        try {
                            mService.challengeResponse(input.getText().toString());
                            getActivity().finish();
                        } catch (RemoteException e) {
                            VpnStatus.logException(e);
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel,
                        (dialog, whichButton) -> getActivity().finish()
                )
                .create();
    }
}