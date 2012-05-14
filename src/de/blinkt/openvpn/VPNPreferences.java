package de.blinkt.openvpn;

import java.util.List;

import android.os.Bundle;
import android.preference.PreferenceActivity;


public class VPNPreferences extends PreferenceActivity {

	private String mProfileUUID;

	public VPNPreferences() {
		super();
	
	}
	


	
	@Override
	protected void onStop() {
		super.onStop();
	};
	
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mProfileUUID = getIntent().getStringExtra(getPackageName() + ".profileUUID");
		super.onCreate(savedInstanceState);
	}
	
	
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.vpn_headers, target); 
		for (Header header : target) {
			if(header.fragmentArguments==null)
				header.fragmentArguments = new Bundle();
			header.fragmentArguments.putString(getPackageName() + ".profileUUID",mProfileUUID);
		}
	}
	
	@Override
	public void onBackPressed() {
		setResult(RESULT_OK, getIntent());
		super.onBackPressed();
	}
}

