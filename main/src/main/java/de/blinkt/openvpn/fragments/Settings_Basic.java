/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.security.cert.X509Certificate;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.R.id;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.X509Utils;
import de.blinkt.openvpn.views.FileSelectLayout;

public class Settings_Basic extends Settings_Fragment implements View.OnClickListener, OnItemSelectedListener, Callback, FileSelectLayout.FileSelectCallback {
	private static final int CHOOSE_FILE_OFFSET = 1000;
	private static final int UPDATE_ALIAS = 20;

	private FileSelectLayout mClientCert;
	private FileSelectLayout mCaCert;
	private FileSelectLayout mClientKey;
	private TextView mAliasName;
    private TextView mAliasCertificate;
	private CheckBox mUseLzo;
	private Spinner mType;
	private FileSelectLayout mpkcs12;
	private FileSelectLayout mCrlFile;
	private TextView mPKCS12Password;
	private Handler mHandler;
	private EditText mUserName;
	private EditText mPassword;
	private View mView;
	private EditText mProfileName;
	private EditText mKeyPassword;

	private SparseArray<FileSelectLayout> fileselects = new SparseArray<>();


	private void addFileSelectLayout (FileSelectLayout fsl, Utils.FileType type) {
		int i = fileselects.size() + CHOOSE_FILE_OFFSET;
		fileselects.put(i, fsl);
		fsl.setCaller(this, i, type);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}


    private void setKeystoreCertficate()
    {
        new Thread() {
            public void run() {
                String certstr="";
                try {
                    X509Certificate cert = KeyChain.getCertificateChain(getActivity(), mProfile.mAlias)[0];

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        {
                            if (isInHardwareKeystore())
                                certstr+=getString(R.string.hwkeychain);
                        }
                    }
					certstr+=X509Utils.getCertificateValidityString(cert, getResources());
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
        String algorithm = KeyChain.getPrivateKey(getActivity(), mProfile.mAlias).getAlgorithm();
        return KeyChain.isBoundKeyAlgorithm(algorithm);
    }

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


		mView = inflater.inflate(R.layout.basic_settings,container,false);

		mProfileName = (EditText) mView.findViewById(R.id.profilename);
		mClientCert = (FileSelectLayout) mView.findViewById(R.id.certselect);
		mClientKey = (FileSelectLayout) mView.findViewById(R.id.keyselect);
		mCaCert = (FileSelectLayout) mView.findViewById(R.id.caselect);
		mpkcs12 = (FileSelectLayout) mView.findViewById(R.id.pkcs12select);
		mCrlFile = (FileSelectLayout) mView.findViewById(id.crlfile);
		mUseLzo = (CheckBox) mView.findViewById(R.id.lzo);
		mType = (Spinner) mView.findViewById(R.id.type);
		mPKCS12Password = (TextView) mView.findViewById(R.id.pkcs12password);
		mAliasName = (TextView) mView.findViewById(R.id.aliasname);
        mAliasCertificate = (TextView) mView.findViewById(id.alias_certificate);

		mUserName = (EditText) mView.findViewById(R.id.auth_username);
		mPassword = (EditText) mView.findViewById(R.id.auth_password);
		mKeyPassword = (EditText) mView.findViewById(R.id.key_password);

		addFileSelectLayout(mCaCert, Utils.FileType.CA_CERTIFICATE);
		addFileSelectLayout(mClientCert, Utils.FileType.CLIENT_CERTIFICATE);
		addFileSelectLayout(mClientKey, Utils.FileType.KEYFILE);
		addFileSelectLayout(mpkcs12, Utils.FileType.PKCS12);
		addFileSelectLayout(mCrlFile, Utils.FileType.CRL_FILE);
		mCaCert.setShowClear();

		mType.setOnItemSelectedListener(this);

		mView.findViewById(R.id.select_keystore_button).setOnClickListener(this);


		if (mHandler == null) {
			mHandler = new Handler(this);
		}

		return mView;
	}


	@Override
	public void onStart() {
		super.onStart();
		String profileUuid =getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile=ProfileManager.get(getActivity(),profileUuid);
		loadPreferences();

	}

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (result == Activity.RESULT_OK && request >= CHOOSE_FILE_OFFSET) {

			FileSelectLayout fsl = fileselects.get(request);
            fsl.parseResponse(data, getActivity());

			savePreferences();

			// Private key files may result in showing/hiding the private key password dialog
			if(fsl==mClientKey) {
				changeType(mType.getSelectedItemPosition());
			} 
		}

	}


    @Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == mType) {
			changeType(position);
		}
	}


    private void changeType(int type){
		// hide everything
		mView.findViewById(R.id.pkcs12).setVisibility(View.GONE);
		mView.findViewById(R.id.certs).setVisibility(View.GONE);
		mView.findViewById(R.id.statickeys).setVisibility(View.GONE);
		mView.findViewById(R.id.keystore).setVisibility(View.GONE);
		mView.findViewById(R.id.cacert).setVisibility(View.GONE);
        ((FileSelectLayout) mView.findViewById(R.id.caselect)).setClearable(false);
        mView.findViewById(R.id.userpassword).setVisibility(View.GONE);
		mView.findViewById(R.id.key_password_layout).setVisibility(View.GONE);

		// Fall through are by design
		switch(type) {
		case VpnProfile.TYPE_USERPASS_CERTIFICATES:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
		case VpnProfile.TYPE_CERTIFICATES:
			mView.findViewById(R.id.certs).setVisibility(View.VISIBLE);
			mView.findViewById(R.id.cacert).setVisibility(View.VISIBLE);
			if(mProfile.requireTLSKeyPassword())
				mView.findViewById(R.id.key_password_layout).setVisibility(View.VISIBLE);
			break;

		case VpnProfile.TYPE_USERPASS_PKCS12:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
		case VpnProfile.TYPE_PKCS12:
			mView.findViewById(R.id.pkcs12).setVisibility(View.VISIBLE);
			break;

		case VpnProfile.TYPE_STATICKEYS:
			mView.findViewById(R.id.statickeys).setVisibility(View.VISIBLE);
			break;

		case VpnProfile.TYPE_USERPASS_KEYSTORE:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
		case VpnProfile.TYPE_KEYSTORE:
			mView.findViewById(R.id.keystore).setVisibility(View.VISIBLE);
			mView.findViewById(R.id.cacert).setVisibility(View.VISIBLE);
            ((FileSelectLayout) mView.findViewById(R.id.caselect)).setClearable(true);
            break;

		case VpnProfile.TYPE_USERPASS:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
			mView.findViewById(R.id.cacert).setVisibility(View.VISIBLE);
			break;
		}


	}

	private void loadPreferences() {
		mProfileName.setText(mProfile.mName);
		mClientCert.setData(mProfile.mClientCertFilename, getActivity());
		mClientKey.setData(mProfile.mClientKeyFilename, getActivity());
		mCaCert.setData(mProfile.mCaFilename, getActivity());
        mCrlFile.setData(mProfile.mCrlFilename, getActivity());

		mUseLzo.setChecked(mProfile.mUseLzo);
		mType.setSelection(mProfile.mAuthenticationType);
		mpkcs12.setData(mProfile.mPKCS12Filename, getActivity());
		mPKCS12Password.setText(mProfile.mPKCS12Password);
		mUserName.setText(mProfile.mUsername);
		mPassword.setText(mProfile.mPassword);
		mKeyPassword.setText(mProfile.mKeyPassword);

		setAlias();

	}

	protected void savePreferences() {

		mProfile.mName = mProfileName.getText().toString();
		mProfile.mCaFilename = mCaCert.getData();
		mProfile.mClientCertFilename = mClientCert.getData();
		mProfile.mClientKeyFilename = mClientKey.getData();
        mProfile.mCrlFilename = mCrlFile.getData();

		mProfile.mUseLzo = mUseLzo.isChecked();
		mProfile.mAuthenticationType = mType.getSelectedItemPosition();
		mProfile.mPKCS12Filename = mpkcs12.getData();
		mProfile.mPKCS12Password = mPKCS12Password.getText().toString();

		mProfile.mPassword = mPassword.getText().toString();
		mProfile.mUsername = mUserName.getText().toString();
		mProfile.mKeyPassword = mKeyPassword.getText().toString();

	}


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

	@SuppressWarnings("WrongConstant")
    public void showCertDialog () {
		try	{
			KeyChain.choosePrivateKeyAlias(getActivity(),
					new KeyChainAliasCallback() {

				public void alias(String alias) {
					// Credential alias selected.  Remember the alias selection for future use.
					mProfile.mAlias=alias;
					mHandler.sendEmptyMessage(UPDATE_ALIAS);
				}


			},
			new String[] {"RSA"}, // List of acceptable key types. null for any
			null,                        // issuer, null for any
			mProfile.mServerName,      // host name of server requesting the cert, null if unavailable
			-1,                         // port of server requesting the cert, -1 if unavailable
			mProfile.mAlias);                       // alias to preselect, null if unavailable
		} catch (ActivityNotFoundException anf) {
			Builder ab = new AlertDialog.Builder(getActivity());
			ab.setTitle(R.string.broken_image_cert_title);
			ab.setMessage(R.string.broken_image_cert);
			ab.setPositiveButton(android.R.string.ok, null);
			ab.show();
		}
	}

	@Override
	public void onClick(View v) {
		if (v == mView.findViewById(R.id.select_keystore_button)) {
			showCertDialog();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		savePreferences();
		if(mProfile!=null) {
			outState.putString(getActivity().getPackageName() + "profileUUID", mProfile.getUUID().toString());
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}


	@Override
	public boolean handleMessage(Message msg) {
		setAlias();
		return true;
	}


}
