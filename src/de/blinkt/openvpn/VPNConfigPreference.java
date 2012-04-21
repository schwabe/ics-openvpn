package de.blinkt.openvpn;

import android.content.Context;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.util.AttributeSet;

public class VPNConfigPreference extends Preference {

	public VPNConfigPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	setWidgetLayoutResource(R.layout.vpn_preference_layout);
	}

}
