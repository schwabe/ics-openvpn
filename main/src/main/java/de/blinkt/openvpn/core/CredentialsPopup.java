/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.InputType;
import android.widget.EditText;
import de.blinkt.openvpn.R;

public class CredentialsPopup extends Activity {
    public static final String EXTRA_CHALLENGE_TXT = "de.blinkt.openvpn.core.CR_TEXT_CHALLENGE";




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the alarm ID from the intent extra data
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            finish();
            return;
        }
        String challenge = extras.getString(EXTRA_CHALLENGE_TXT, "(empty challenge text)");

        showPwDialog(challenge);
    }




    private void showPwDialog(String challenge) {
        DialogFragment frag = PasswordDialogFragment.newInstance(challenge);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        frag.show(ft, "dialog");
    }
}
