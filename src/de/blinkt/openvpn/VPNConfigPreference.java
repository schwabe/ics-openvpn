package de.blinkt.openvpn;

import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class VPNConfigPreference extends Preference implements OnClickListener {
	class startClickListener implements OnClickListener{
		private VPNConfigPreference mvp;

		public startClickListener(VPNConfigPreference vp) {
			mvp = vp;
		}

		@Override
		public void onClick(View v) {
			mOnQuickSettingsListener.onStartVPNClick(mvp);
		}
		
	}
	
	private VpnPreferencesClickListener mOnQuickSettingsListener;
	private ImageView mQuickPrefButton;


	public VPNConfigPreference(VPNProfileList vpnProfileList) {
		super(vpnProfileList.getActivity());
		setLayoutResource(R.layout.vpn_preference_layout);
	
	}
	
		private View mProfilesPref;

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mProfilesPref = view.findViewById(R.id.vpnconfig_pref);
        mProfilesPref.setOnClickListener(new startClickListener(this));
        mProfilesPref.setClickable(true);
		
		mQuickPrefButton = (ImageView) view.findViewById(R.id.quickedit_settings);
		mQuickPrefButton.setOnClickListener(this);
		
	}
	

	public interface VpnPreferencesClickListener {
		/**
		 * Called when a Preference has been clicked.
		 *
		 * @param preference The Preference that was clicked.
		 * @return True if the click was handled.
		 */
		boolean onQuickSettingsClick(Preference preference);

		void onStartVPNClick(VPNConfigPreference vpnConfigPreference);
	}

	

	public void setOnQuickSettingsClickListener(VpnPreferencesClickListener onQuickSettingsListener) {
		mOnQuickSettingsListener = onQuickSettingsListener;
	}

	@Override
	public void onClick(View v) {
		mOnQuickSettingsListener.onQuickSettingsClick(this);
	}
	

}
