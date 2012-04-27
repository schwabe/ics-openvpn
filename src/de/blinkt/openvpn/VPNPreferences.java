package de.blinkt.openvpn;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Button;


public class VPNPreferences extends PreferenceActivity {

	private VpnProfile mProfile;
	private BasicSettings mBS;
	public void setmBS(BasicSettings mBS) {
		this.mBS = mBS;
	}

	public VPNPreferences() {
		super();
	
	}
	

	protected void onPause() {
		super.onPause();
		saveSettings();

	}
	
	
	@Override
	protected void onStop() {
		super.onStop();
	};
	
	
	private void saveSettings() {
		// First let basic settings save its state
		if(mBS!=null)
			mBS.savePreferences();
		
		ObjectOutputStream vpnfile;
		try {
			vpnfile = new ObjectOutputStream(openFileOutput((mProfile.getUUID().toString() + ".vp"),Activity.MODE_PRIVATE));

			vpnfile.writeObject(mProfile);
			vpnfile.flush();
			vpnfile.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(getPackageName() + ".VpnProfile",mProfile);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		mProfile = (VpnProfile) state.getSerializable(getPackageName() + ".VpnProfile");
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mProfile = (VpnProfile) getIntent().getSerializableExtra(getPackageName() + ".VpnProfile");
		super.onCreate(savedInstanceState);

	
				
		if (hasHeaders()) {
			Button button = new Button(this);
			button.setText("Save");
			setListFooter(button);
		}
	}
	
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.vpn_headers, target); 
		for (Header header : target) {
			if(header.fragmentArguments==null)
				header.fragmentArguments = new Bundle();
			header.fragmentArguments.putString(getPackageName() + ".profileUUID",mProfile.getUUID().toString());
			if(header.extras==null)
				header.extras = new Bundle();
			header.extras.putString(getPackageName() + ".profileUUID",mProfile.getUUID().toString());
		}
	}
}

