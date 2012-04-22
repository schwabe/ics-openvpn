package de.blinkt.openvpn;

import java.util.List;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.widget.Button;


public class VPNPreferences extends PreferenceActivity {

	private VpnProfile mProfile;
	public VPNPreferences() {
		super();
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mProfile = getIntent().getParcelableExtra("VpnProfile");
				
		if (hasHeaders()) {
			Button button = new Button(this);
			button.setText("Some action");
			setListFooter(button);
		}
	}
	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.vpn_headers, target);   
	}

	public static class IP_Settings extends PreferenceFragment implements OnPreferenceChangeListener {
		private EditTextPreference mIPv4;
		private EditTextPreference mIPv6;
		private SwitchPreference mUsePull;
		private CheckBoxPreference mOverrideDNS;
		private Preference mSearchdomain;
		private Preference mDNS1;
		private Preference mDNS2;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			
			// Make sure default values are applied.  In a real app, you would
			// want this in a shared function that is used to retrieve the
			// SharedPreferences wherever they are needed.
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.vpn_ipsettings, false);
			
			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.vpn_ipsettings);
			mIPv4 = (EditTextPreference) findPreference("ipv4_address");
			mIPv6 = (EditTextPreference) findPreference("ipv6_address");
			mUsePull = (SwitchPreference) findPreference("usePull");
			mOverrideDNS = (CheckBoxPreference) findPreference("overrideDNS");
			mSearchdomain =findPreference("searchdomain");
			mDNS1 = findPreference("dns1");
			mDNS2 = findPreference("dns2");

			mIPv4.setOnPreferenceChangeListener(this);
			mIPv6.setOnPreferenceChangeListener(this);
			mDNS1.setOnPreferenceChangeListener(this);
			mDNS2.setOnPreferenceChangeListener(this);
			mUsePull.setOnPreferenceChangeListener(this);
			mOverrideDNS.setOnPreferenceChangeListener(this);
			
			
			VpnProfile vp = ((VPNPreferences) getActivity()).getVPNProfile();
			
			
			setDNSState();

			
		}

		@Override
		public boolean onPreferenceChange(Preference preference,
				Object newValue) {
			if(preference==mIPv4 || preference == mIPv6 ||
					preference==mDNS1 || preference == mDNS2)
				preference.setSummary((String)newValue);

			if(preference== mUsePull || preference == mOverrideDNS)
				setDNSState();

			return true;
		}

		private void setDNSState() {
			boolean enabled;
			mOverrideDNS.setEnabled(mUsePull.isChecked());
			if(!mUsePull.isChecked())
				enabled =true;
			else if (mOverrideDNS.isChecked())
				enabled = true;
			else
				enabled = false;

			mDNS1.setEnabled(enabled);
			mDNS2.setEnabled(enabled);
			mSearchdomain.setEnabled(enabled);

		}


	}
	public static class Authentication extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Make sure default values are applied.  In a real app, you would
			// want this in a shared function that is used to retrieve the
			// SharedPreferences wherever they are needed.
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.vpn_authentification, false);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.vpn_authentification);
		}
	}
	public static class Obscure extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// Make sure default values are applied.  In a real app, you would
			// want this in a shared function that is used to retrieve the
			// SharedPreferences wherever they are needed.
			PreferenceManager.setDefaultValues(getActivity(),
					R.xml.vpn_ipsettings, false);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.vpn_obscure);
		}
	}
	public VpnProfile getVPNProfile() {
		return mProfile;
	}
}

