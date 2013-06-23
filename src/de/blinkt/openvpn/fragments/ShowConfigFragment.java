package de.blinkt.openvpn.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;


public class ShowConfigFragment extends Fragment {
	private String configtext;
	public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		final VpnProfile vp = ProfileManager.get(profileUUID);
		View v=inflater.inflate(R.layout.viewconfig, container,false);
		final TextView cv = (TextView) v.findViewById(R.id.configview);
		
		int check=vp.checkProfile(getActivity());
		if(check!=R.string.no_error_found) {
			cv.setText(check);
			configtext = getString(check);
		}
		else {
			// Run in own Thread since Keystore does not like to be queried from the main thread

			cv.setText("Generating config...");
			startGenConfig(vp, cv);
		}
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
}
