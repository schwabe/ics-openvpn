package de.blinkt.openvpn;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ShowConfigFragment extends Fragment {
	public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		String profileUUID = getArguments().getString(getActivity().getPackageName() + ".profileUUID");
		VpnProfile vp = ProfileManager.get(profileUUID);
		String cfg=vp.getConfigFile(getActivity().getCacheDir());
		View v=inflater.inflate(R.layout.viewconfig, container,false);
		TextView cv = (TextView) v.findViewById(R.id.configview);
		cv.setText(cfg);
		return v;
	};
}
