/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.blinkt.openvpn;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.lamerman.FileDialog;

import de.blinkt.openvpn.R.id;

public class OpenVPNClient extends Activity implements View.OnClickListener, OnItemSelectedListener, Callback, OnCheckedChangeListener {
	private static final String TAG = "OpenVpnClient";


	private static final int START_OPENVPN = 0;
	private static final int CHOOSE_FILE_OFFSET = 1000;
	private static final int UPDATE_ALIAS = 20;

	private static final String PREFS_NAME = "OVPN_SERVER";



	private TextView mServerAddress;
	private TextView mServerPort;
	private FileSelectLayout mClientCert;
	private FileSelectLayout mCaCert;
	private FileSelectLayout mClientKey;
	private TextView mAliasName;
	private CheckBox mUseLzo;
	private ToggleButton mTcpUdp;
	private Spinner mType;
	private String certalias;
	private FileSelectLayout mpkcs12;
	private TextView mPKCS12Password;

	private Handler mHandler;


	private CheckBox mUseTlsAuth;


	private CheckBox mShowAdvanced;


	private FileSelectLayout mTlsFile;

	private HashMap<Integer, FileSelectLayout> fileselects = new HashMap<Integer, FileSelectLayout>();


	private Spinner mTLSDirection;


	private EditText mUserName;


	private EditText mPassword;

	@Override
	protected void onStop(){
		super.onStop();
		savePreferences();
	}


	
	private void addFileSelectLayout (FileSelectLayout fsl) {
		int i = fileselects.size() + CHOOSE_FILE_OFFSET;
		fileselects.put(i, fsl);
		fsl.setActivity(this,i);
	}





	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basic_settings);

		// Forces early JNI Load
		OpenVPN.foo();

		mServerAddress = (TextView) findViewById(R.id.address);
		mServerPort = (TextView) findViewById(R.id.port);
		mClientCert = (FileSelectLayout) findViewById(R.id.certselect);
		mClientKey = (FileSelectLayout) findViewById(R.id.keyselect);
		mCaCert = (FileSelectLayout) findViewById(R.id.caselect);
		mpkcs12 = (FileSelectLayout) findViewById(R.id.pkcs12select);
		mUseLzo = (CheckBox) findViewById(R.id.lzo);
		mTcpUdp = (ToggleButton) findViewById(id.tcpudp);
		mType = (Spinner) findViewById(R.id.type);
		mPKCS12Password = (TextView) findViewById(R.id.pkcs12password);
		mAliasName = (TextView) findViewById(R.id.aliasname);
		mUseTlsAuth = (CheckBox) findViewById(R.id.useTLSAuth);
		mTLSDirection = (Spinner) findViewById(R.id.tls_direction);

		mShowAdvanced = (CheckBox) findViewById(R.id.show_advanced);
		mTlsFile = (FileSelectLayout) findViewById(R.id.tlsAuth);		
		mUserName = (EditText) findViewById(R.id.auth_username);
		mPassword = (EditText) findViewById(R.id.auth_password);
		

		addFileSelectLayout(mCaCert);
		addFileSelectLayout(mClientCert);
		addFileSelectLayout(mClientKey);
		addFileSelectLayout(mTlsFile);
		addFileSelectLayout(mpkcs12);

		loadPreferences();

		mType.setOnItemSelectedListener(this);

		mShowAdvanced.setOnCheckedChangeListener(this);
		mUseTlsAuth.setOnCheckedChangeListener(this);


		findViewById(R.id.select_keystore_button).setOnClickListener(this);
		findViewById(R.id.about).setOnClickListener(this);
		findViewById(R.id.connect).setOnClickListener(this);		

		if (mHandler == null) {
			mHandler = new Handler(this);
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
		findViewById(R.id.pkcs12).setVisibility(View.GONE);
		findViewById(R.id.certs).setVisibility(View.GONE);
		findViewById(R.id.statickeys).setVisibility(View.GONE);
		findViewById(R.id.keystore).setVisibility(View.GONE);

		switch(type) {
		case VpnProfile.TYPE_CERTIFICATES:
			findViewById(R.id.certs).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_PKCS12:
			findViewById(R.id.pkcs12).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_STATICKEYS:
			findViewById(R.id.statickeys).setVisibility(View.VISIBLE);
			break;
		case VpnProfile.TYPE_KEYSTORE:
			findViewById(R.id.keystore).setVisibility(View.VISIBLE);
			break;
			
		case VpnProfile.TYPE_USERPASS:
			findViewById(R.id.userpassword).setVisibility(View.VISIBLE);
		}


	}

	private void loadPreferences() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME,Activity.MODE_PRIVATE);

		mClientCert.setData(settings.getString("clientcert", ""));
		mClientKey.setData(settings.getString("clientkey", ""));
		mCaCert.setData(settings.getString("ca", ""));

		mUseLzo.setChecked(settings.getBoolean("useLzo", true));
		mServerPort.setText(settings.getString("port", "1194"));
		mServerAddress.setText(settings.getString("server", "openvpn.blinkt.de"));
		mTcpUdp.setChecked(settings.getBoolean("udp", true));
		mType.setSelection(settings.getInt("type", VpnProfile.TYPE_PKCS12));
		mpkcs12.setData(settings.getString("pkcs12file", ""));
		mPKCS12Password.setText(settings.getString("pkcs12password", ""));
		certalias = settings.getString("alias", null);
		mUseTlsAuth.setChecked(settings.getBoolean("tlsauth", false));
		onCheckedChanged(mUseTlsAuth,mUseTlsAuth.isChecked());
		
		mTlsFile.setData(settings.getString("tlsfile",""));
		mTLSDirection.setSelection(settings.getInt("tls-direction", 2)); // Unspecified
		setAlias();

	}

	private void savePreferences() {
		// We need an Editor object to make preference changes.
		// All objects are from android.context.Context
		SharedPreferences settings = getSharedPreferences(PREFS_NAME,Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();


		editor.putString("ca" , mCaCert.getData());
		editor.putString("clientcert", mClientCert.getData());
		editor.putString("clientkey", mClientKey.getData());

		editor.putBoolean("useLzo",mUseLzo.isChecked());
		editor.putString("port",  mServerPort.getText().toString());
		editor.putString("server", mServerAddress.getText().toString());
		editor.putBoolean("udp", mTcpUdp.isChecked());

		editor.putInt("type",mType.getSelectedItemPosition());
		editor.putString("pkcs12file", mpkcs12.getData());
		editor.putString("pkcs12password", mPKCS12Password.getText().toString());
		editor.putString("alias", certalias);
		editor.putBoolean("tlsauth", mUseTlsAuth.isChecked());
		editor.putString("tlsfile", mTlsFile.getData());
		editor.putInt("tls-direction", mTLSDirection.getSelectedItemPosition());
		// Commit the edits!
		editor.commit();
	}


	private void setAlias() {
		if(certalias == null) {
			mAliasName.setText(R.string.client_no_certificate);
		} else {
			mAliasName.setText(certalias);
		}
	}

	public void showCertDialog () {
		KeyChain.choosePrivateKeyAlias(this,
				new KeyChainAliasCallback() {

			public void alias(String alias) {
				// Credential alias selected.  Remember the alias selection for future use.
				certalias=alias;
				mHandler.sendEmptyMessage(UPDATE_ALIAS);
			}


		},
		new String[] {"RSA", "DSA"}, // List of acceptable key types. null for any
		null,                        // issuer, null for any
		"internal.example.com",      // host name of server requesting the cert, null if unavailable
		443,                         // port of server requesting the cert, -1 if unavailable
		null);                       // alias to preselect, null if unavailable
	}

	

	public void testGetallCerts() throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		tmf.init((KeyStore) null);
		X509TrustManager xtm = (X509TrustManager) tmf.getTrustManagers()[0];

		X509Certificate[] foo = xtm.getAcceptedIssuers();
		for (X509Certificate cert : xtm.getAcceptedIssuers()) {
			String certStr = "S:" + cert.getSubjectDN().getName() + "\nI:"
					+ cert.getIssuerDN().getName();
			Log.d(TAG, certStr);
		}
		System.out.println(foo);
	}

	@Override
	public void onClick(View v) {
		if(v == findViewById(R.id.connect)) {
			Intent intent = VpnService.prepare(this);
			if (intent != null) {
				startActivityForResult(intent, 0);
			} else {
				onActivityResult(START_OPENVPN, RESULT_OK, null);
			}
		} else if (v == findViewById(R.id.about)) {
			//Intent intent = new Intent(getBaseContext(),AboutActivity.class);
			Intent intent = new Intent(getBaseContext(),VPNPreferences.class);
			intent.putExtra("foo","der bar war hier!");
			startActivity(intent);
		} else if (v == findViewById(R.id.select_keystore_button)) {
			showCertDialog();
		}
	}



	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int request, int result, Intent data) {
		if (request== START_OPENVPN) {
			if (result == RESULT_OK) {
				// It always crashes and never saves ;)
				savePreferences();
				new startOpenVpnThread().start();
			}

		} else if (request >= CHOOSE_FILE_OFFSET) {
			String filepath = data.getStringExtra(FileDialog.RESULT_PATH);
			FileSelectLayout fsl = fileselects.get(request);
			fsl.setData(filepath);
		}
		savePreferences();
	}



	private class startOpenVpnThread extends Thread {

		@Override
		public void run() {
		//	startOpenVpn();
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


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		int visibility;
		if(isChecked) 
			visibility =View.VISIBLE;
		else 
			visibility =View.GONE;

		if(buttonView==mShowAdvanced) {
			findViewById(R.id.advanced_options).setVisibility(visibility);
		} else if (buttonView == mUseTlsAuth) {
			findViewById(R.id.tlsauth_options).setVisibility(visibility);
		}
	}
}
