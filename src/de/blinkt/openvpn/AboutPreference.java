package de.blinkt.openvpn;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class AboutPreference extends Preference {

	public AboutPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setWidgetLayoutResource(R.layout.about);
	}

}
