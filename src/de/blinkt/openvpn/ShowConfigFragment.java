package de.blinkt.openvpn;

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


public class ShowConfigFragment extends Fragment {
	private String configtext;
	public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		VpnProfile vp = ProfileManager.get(profileUUID);
		View v=inflater.inflate(R.layout.viewconfig, container,false);
		TextView cv = (TextView) v.findViewById(R.id.configview);
		
		int check=vp.checkProfile(getActivity());
		if(check!=R.string.no_error_found) {
			cv.setText(check);
			configtext = getString(check);
		}
		else { 
			String cfg=vp.getConfigFile(getActivity());
			configtext= cfg;
			cv.setText(cfg);
		}
		return v;
	};

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
