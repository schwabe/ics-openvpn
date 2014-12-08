/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.app.Fragment;
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
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;


public class ShowConfigFragment extends Fragment {
	private String configtext;
    private TextView mConfigView;

    public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View v=inflater.inflate(R.layout.viewconfig, container,false);
		mConfigView = (TextView) v.findViewById(R.id.configview);
		

        ImageButton fabButton = (ImageButton) v.findViewById(R.id.share_config);
        if (fabButton!=null)
            fabButton.setOnClickListener( new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareConfig();
                }
            });


		return v;
	}

	private void startGenConfig(final VpnProfile vp, final TextView cv) {
		
		new Thread() {
			public void run() {
				final String cfg=vp.getConfigFile(getActivity(),false);
				configtext= cfg;
				getActivity().runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						cv.setText(cfg);		
					}
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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
		    inflater.inflate(R.menu.configmenu, menu);
	}
	
	private void shareConfig() {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, configtext);
		shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_config_title));
		shareIntent.setType("text/plain");
		startActivity(Intent.createChooser(shareIntent, "Export Configfile"));
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

        String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
        final VpnProfile vp = ProfileManager.get(getActivity(),profileUUID);
        int check=vp.checkProfile(getActivity());

        if(check!=R.string.no_error_found) {
            mConfigView.setText(check);
            configtext = getString(check);
        }
        else {
            // Run in own Thread since Keystore does not like to be queried from the main thread

            mConfigView.setText("Generating config...");
            startGenConfig(vp, mConfigView);
        }
    }
}
