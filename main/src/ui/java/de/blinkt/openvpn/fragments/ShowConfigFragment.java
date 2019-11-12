/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;


public class ShowConfigFragment extends Fragment {
	private String configtext;
    private TextView mConfigView;
	private ImageButton mfabButton;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v=inflater.inflate(R.layout.viewconfig, container,false);
		mConfigView = v.findViewById(R.id.configview);


		mfabButton = v.findViewById(R.id.share_config);
        if (mfabButton!=null) {
			mfabButton.setOnClickListener(v1 -> shareConfig());
			mfabButton.setVisibility(View.INVISIBLE);
		}
		return v;
	}

	private void startGenConfig(final VpnProfile vp, final TextView cv) {

		new Thread() {
			public void run() {
				/* Add a few newlines to make the textview scrollable past the FAB */
				try {

					configtext = vp.getConfigFile(requireContext(), VpnProfile.doUseOpenVPN3(getActivity())) + "\n\n\n";
				} catch (Exception e) {
					e.printStackTrace();
					configtext = "Error generating config file: " + e.getLocalizedMessage();
				}
				requireActivity().runOnUiThread(() -> {
                    cv.setText(configtext);
					if (mfabButton!=null)
                        mfabButton.setVisibility(View.VISIBLE);
                });


			}
		}.start();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
		    inflater.inflate(R.menu.configmenu, menu);
	}

	private void shareConfig() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, configtext);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_config_title));
		shareIntent.setType("text/plain");
		startActivity(Intent.createChooser(shareIntent, getString(R.string.export_config_chooser_title)));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == R.id.sendConfig) {
			shareConfig();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

    @Override
    public void onResume() {
        super.onResume();

		populateConfigText();
    }

	private void populateConfigText() {
		String profileUUID = requireArguments().getString(requireActivity().getPackageName() + ".profileUUID");
		final VpnProfile vp = ProfileManager.get(getActivity(),profileUUID);
		int check=vp.checkProfile(getActivity());

		if(check != R.string.no_error_found) {
            mConfigView.setText(check);
            configtext = getString(check);
        }
        else {
            // Run in own Thread since Keystore does not like to be queried from the main thread

            mConfigView.setText("Generating config...");
            startGenConfig(vp, mConfigView);
        }
	}

	@Override
	public void setUserVisibleHint(boolean visible)
	{
		super.setUserVisibleHint(visible);
		if (visible && isResumed())
			populateConfigText();
	}
}
