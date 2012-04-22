package de.blinkt.openvpn;

import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class VPNConfigPreference extends Preference implements OnClickListener {


	private OnQuickSettingsClickListener mOnQuickSettingsListener;
	private ImageView mQuickPrefButton;


	public VPNConfigPreference(VPNProfileList vpnProfileList, Bundle args) {

		super(vpnProfileList.getActivity());
		setWidgetLayoutResource(R.layout.vpn_preference_layout);


	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mQuickPrefButton = (ImageView) view.findViewById(R.id.quickedit_settings);
		mQuickPrefButton.setOnClickListener(this);
	}

	public interface OnQuickSettingsClickListener {
		/**
		 * Called when a Preference has been clicked.
		 *
		 * @param preference The Preference that was clicked.
		 * @return True if the click was handled.
		 */
		boolean onQuickSettingsClick(Preference preference);
	}


	public void setOnQuickSettingsClickListener(OnQuickSettingsClickListener onQuickSettingsListener) {
		mOnQuickSettingsListener = onQuickSettingsListener;
	}

	@Override
	public void onClick(View v) {
		if (mOnQuickSettingsListener != null) {
			mOnQuickSettingsListener.onQuickSettingsClick(this);
		}

	}


}
