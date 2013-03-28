package de.blinkt.openvpn.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
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
import android.widget.ToggleButton;
import de.blinkt.openvpn.FileSelect;
import de.blinkt.openvpn.FileSelectLayout;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.R.id;
import de.blinkt.openvpn.core.ProfileManager;

public class Settings_Basic extends Fragment implements View.OnClickListener, OnItemSelectedListener, Callback {
	private static final int CHOOSE_FILE_OFFSET = 1000;
	private static final int UPDATE_ALIAS = 20;



	private TextView mServerAddress;
	private TextView mServerPort;
	private FileSelectLayout mClientCert;
	private FileSelectLayout mCaCert;
	private FileSelectLayout mClientKey;
	private TextView mAliasName;
	private CheckBox mUseLzo;
	private ToggleButton mTcpUdp;
	private Spinner mType;
	private FileSelectLayout mpkcs12;
	private TextView mPKCS12Password;
	private Handler mHandler;
	private EditText mUserName;
	private EditText mPassword;
	private View mView;
	private VpnProfile mProfile;
	private EditText mProfileName;
	private EditText mKeyPassword;

	private SparseArray<FileSelectLayout> fileselects = new SparseArray<FileSelectLayout>();


	private void addFileSelectLayout (FileSelectLayout fsl) {
		int i = fileselects.size() + CHOOSE_FILE_OFFSET;
		fileselects.put(i, fsl);
		fsl.setFragment(this,i);
	}


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String profileuuid =getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile=ProfileManager.get(profileuuid);
		getActivity().setTitle(getString(R.string.edit_profile_title, mProfile.getName()));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


		mView = inflater.inflate(R.layout.basic_settings,container,false);

		mProfileName = (EditText) mView.findViewById(R.id.profilename);
		mServerAddress = (TextView) mView.findViewById(R.id.address);
		mServerPort = (TextView) mView.findViewById(R.id.port);
		mClientCert = (FileSelectLayout) mView.findViewById(R.id.certselect);
		mClientKey = (FileSelectLayout) mView.findViewById(R.id.keyselect);
		mCaCert = (FileSelectLayout) mView.findViewById(R.id.caselect);
		mpkcs12 = (FileSelectLayout) mView.findViewById(R.id.pkcs12select);
		mUseLzo = (CheckBox) mView.findViewById(R.id.lzo);
		mTcpUdp = (ToggleButton) mView.findViewById(id.tcpudp);
		mType = (Spinner) mView.findViewById(R.id.type);
		mPKCS12Password = (TextView) mView.findViewById(R.id.pkcs12password);
		mAliasName = (TextView) mView.findViewById(R.id.aliasname);

		mUserName = (EditText) mView.findViewById(R.id.auth_username);
		mPassword = (EditText) mView.findViewById(R.id.auth_password);
		mKeyPassword = (EditText) mView.findViewById(R.id.key_password);



		addFileSelectLayout(mCaCert);
		addFileSelectLayout(mClientCert);
		addFileSelectLayout(mClientKey);
		addFileSelectLayout(mpkcs12);
		mpkcs12.setBase64Encode();
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
		String profileuuid =getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		mProfile=ProfileManager.get(profileuuid);
		loadPreferences();

	}

	@Override
	public void onActivityResult(int request, int result, Intent data) {
		if (result == Activity.RESULT_OK && request >= CHOOSE_FILE_OFFSET) {
			String filedata = data.getStringExtra(FileSelect.RESULT_DATA);
			FileSelectLayout fsl = fileselects.get(request);
			fsl.setData(filedata);

			savePreferences();

			// Private key files may result in showing/hiding the private key password dialog
			if(fsl==mClientKey) {
				changeType(mType.getSelectedItemPosition());
			}
		}

	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
		if (parent == mType) {
			changeType(position);
		}
	}
	@Override
	public void onPause() {
		super.onPause();
		savePreferences();
	}



	private void changeType(int type){
		// hide everything
		mView.findViewById(R.id.pkcs12).setVisibility(View.GONE);
		mView.findViewById(R.id.certs).setVisibility(View.GONE);
		mView.findViewById(R.id.statickeys).setVisibility(View.GONE);
		mView.findViewById(R.id.keystore).setVisibility(View.GONE);
		mView.findViewById(R.id.cacert).setVisibility(View.GONE);
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
			break;

		case VpnProfile.TYPE_USERPASS:
			mView.findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
			mView.findViewById(R.id.cacert).setVisibility(View.VISIBLE);
			break;
		}


	}

	private void loadPreferences() {
		mProfileName.setText(mProfile.mName);
		mClientCert.setData(mProfile.mClientCertFilename);
		mClientKey.setData(mProfile.mClientKeyFilename);
		mCaCert.setData(mProfile.mCaFilename);

		mUseLzo.setChecked(mProfile.mUseLzo);
		mServerPort.setText(mProfile.mServerPort);
		mServerAddress.setText(mProfile.mServerName);
		mTcpUdp.setChecked(mProfile.mUseUdp);
		mType.setSelection(mProfile.mAuthenticationType);
		mpkcs12.setData(mProfile.mPKCS12Filename);
		mPKCS12Password.setText(mProfile.mPKCS12Password);
		mUserName.setText(mProfile.mUsername);
		mPassword.setText(mProfile.mPassword);
		mKeyPassword.setText(mProfile.mKeyPassword);

		setAlias();

	}

	void savePreferences() {

		mProfile.mName = mProfileName.getText().toString();
		mProfile.mCaFilename = mCaCert.getData();
		mProfile.mClientCertFilename = mClientCert.getData();
		mProfile.mClientKeyFilename = mClientKey.getData();

		mProfile.mUseLzo = mUseLzo.isChecked();
		mProfile.mServerPort =mServerPort.getText().toString();
		mProfile.mServerName = mServerAddress.getText().toString();
		mProfile.mUseUdp = mTcpUdp.isChecked();

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
		} else {
			mAliasName.setText(mProfile.mAlias);
		}
	}

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
