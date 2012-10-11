package de.blinkt.openvpn;

import java.util.List;

import android.content.Intent;
import android.preference.PreferenceActivity;

public class MainActivity extends PreferenceActivity {

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.main_headers, target);
		

		String translatedby = getString(R.string.translationby);
		if(!translatedby.equals("") && !translatedby.contains("Arne Schwabe")) {
			Header translation = new Header();
			translation.title = getString(R.string.translation);
			translation.summary = translatedby;
			target.add(translation);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		System.out.println(data);
		
		
	}
}
