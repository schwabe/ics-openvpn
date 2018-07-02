/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.view.View;
import android.widget.TextView;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.X509Utils;

import java.security.cert.X509Certificate;

abstract class KeyChainSettingsFragment extends Settings_Fragment implements View.OnClickListener, Handler.Callback {
    private static final int UPDATE_ALIAS = 20;


    private TextView mAliasCertificate;
    private TextView mAliasName;
    private Handler mHandler;



    private void setAlias() {
        if(mProfile.mAlias == null) {
            mAliasName.setText(R.string.client_no_certificate);
            mAliasCertificate.setText("");
        } else {
            mAliasCertificate.setText("Loading certificate from Keystore...");
            mAliasName.setText(mProfile.mAlias);
            setKeystoreCertficate();
        }
    }

    protected void setKeystoreCertficate()
    {
        new Thread() {
            public void run() {
                String certstr="";
                try {
                    X509Certificate cert = KeyChain.getCertificateChain(getActivity().getApplicationContext(), mProfile.mAlias)[0];

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        {
                            if (isInHardwareKeystore())
                                certstr+=getString(R.string.hwkeychain);
                        }
                    }
                    certstr+= X509Utils.getCertificateValidityString(cert, getResources());
                    certstr+=X509Utils.getCertificateFriendlyName(cert);

                } catch (Exception e) {
                    certstr="Could not get certificate from Keystore: " +e.getLocalizedMessage();
                }

                final String certStringCopy=certstr;
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        mAliasCertificate.setText(certStringCopy);
                    }
                });

            }
        }.start();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isInHardwareKeystore() throws KeyChainException, InterruptedException {
        String algorithm = KeyChain.getPrivateKey(getActivity().getApplicationContext(), mProfile.mAlias).getAlgorithm();
        return KeyChain.isBoundKeyAlgorithm(algorithm);
    }

    protected void initKeychainViews(View v) {
        v.findViewById(R.id.select_keystore_button).setOnClickListener(this);
        mAliasCertificate = v.findViewById(R.id.alias_certificate);
        mAliasName = v.findViewById(R.id.aliasname);
        if (mHandler == null) {
            mHandler = new Handler(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == v.findViewById(R.id.select_keystore_button)) {
            showCertDialog();
        }
    }

    @Override
    protected void savePreferences() {

    }

    @Override
    public void onStart() {
        super.onStart();
        loadPreferences();
    }

    @SuppressWarnings("WrongConstant")
    public void showCertDialog () {
        try	{
            KeyChain.choosePrivateKeyAlias(getActivity(),
                    alias -> {
                        // Credential alias selected.  Remember the alias selection for future use.
                        mProfile.mAlias=alias;
                        mHandler.sendEmptyMessage(UPDATE_ALIAS);
                    },
                    new String[] {"RSA"}, // List of acceptable key types. null for any
                    null,                        // issuer, null for any
                    mProfile.mServerName,      // host name of server requesting the cert, null if unavailable
                    -1,                         // port of server requesting the cert, -1 if unavailable
                    mProfile.mAlias);                       // alias to preselect, null if unavailable
        } catch (ActivityNotFoundException anf) {
            AlertDialog.Builder ab = new AlertDialog.Builder(getActivity());
            ab.setTitle(R.string.broken_image_cert_title);
            ab.setMessage(R.string.broken_image_cert);
            ab.setPositiveButton(android.R.string.ok, null);
            ab.show();
        }
    }

    protected void loadPreferences()
    {
        setAlias();
    }

    @Override
    public boolean handleMessage(Message msg) {
        setAlias();
        return true;
    }
}
