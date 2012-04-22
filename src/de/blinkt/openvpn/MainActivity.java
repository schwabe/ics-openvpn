package de.blinkt.openvpn;

import java.util.List;

import android.preference.PreferenceActivity;
import android.preference.PreferenceActivity.Header;

public class MainActivity extends PreferenceActivity {
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.main_headers, target);   
	}
}
