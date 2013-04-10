package de.blinkt.openvpn.fragments;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.blinkt.openvpn.ConfigConverter;
import de.blinkt.openvpn.FileSelect;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VPNPreferences;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ProfileManager;

public class VPNProfileList extends ListFragment {

	public final static int RESULT_VPN_DELETED = Activity.RESULT_FIRST_USER;

	private static final int MENU_ADD_PROFILE = Menu.FIRST;

	private static final int START_VPN_CONFIG = 92;
	private static final int SELECT_PROFILE = 43;
	private static final int IMPORT_PROFILE = 231;

	private static final int MENU_IMPORT_PROFILE = Menu.FIRST +1;

	class VPNArrayAdapter extends ArrayAdapter<VpnProfile> {

		public VPNArrayAdapter(Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);

			View titleview = v.findViewById(R.id.vpn_list_item_left);
			titleview.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					VpnProfile profile =(VpnProfile) getListAdapter().getItem(position);
					startVPN(profile);
				}
			});

			View settingsview = v.findViewById(R.id.quickedit_settings);
			settingsview.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					VpnProfile editProfile = (VpnProfile) getListAdapter().getItem(position);
					editVPN(editProfile);

				}
			});

			return v;
		}
	}








	private ArrayAdapter<VpnProfile> mArrayadapter;

	protected VpnProfile mEditProfile=null;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

	}


	class MiniImageGetter implements ImageGetter {


		@Override
		public Drawable getDrawable(String source) {
			Drawable d=null;
			if ("ic_menu_add".equals(source))
				d = getActivity().getResources().getDrawable(android.R.drawable.ic_menu_add);
			else if("ic_menu_archive".equals(source))
				d = getActivity().getResources().getDrawable(R.drawable.ic_menu_archive);
			
			
			
			if(d!=null) {
				d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
				return d;
			}else{
				return null;
			}
		}
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.vpn_profile_list, container,false);

		TextView newvpntext = (TextView) v.findViewById(R.id.add_new_vpn_hint);
		TextView importvpntext = (TextView) v.findViewById(R.id.import_vpn_hint);
		
		
		
		newvpntext.setText(Html.fromHtml(getString(R.string.add_new_vpn_hint),new MiniImageGetter(),null));
		importvpntext.setText(Html.fromHtml(getString(R.string.vpn_import_hint),new MiniImageGetter(),null));
		
        

		return v;

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setListAdapter();
	}

	class VpnProfileNameComperator implements Comparator<VpnProfile> {

		@Override
		public int compare(VpnProfile lhs, VpnProfile rhs) {
			return lhs.mName.compareTo(rhs.mName);
		}

	}

	private void setListAdapter() {
		mArrayadapter = new VPNArrayAdapter(getActivity(),R.layout.vpn_list_item,R.id.vpn_item_title);
		Collection<VpnProfile> allvpn = getPM().getProfiles();

		TreeSet<VpnProfile> sortedset = new TreeSet<VpnProfile>(new VpnProfileNameComperator()); 
		sortedset.addAll(allvpn);
		mArrayadapter.addAll(sortedset);

		setListAdapter(mArrayadapter);
	}



	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(0, MENU_ADD_PROFILE, 0 , R.string.menu_add_profile)
		.setIcon(android.R.drawable.ic_menu_add)
		.setAlphabeticShortcut('a')
		.setTitleCondensed(getActivity().getString(R.string.add))
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS |  MenuItem.SHOW_AS_ACTION_WITH_TEXT);

		menu.add(0, MENU_IMPORT_PROFILE, 0,  R.string.menu_import)
		.setIcon(R.drawable.ic_menu_archive)
		.setAlphabeticShortcut('i')
		.setTitleCondensed(getActivity().getString(R.string.menu_import_short))
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT );
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == MENU_ADD_PROFILE) {
			onAddProfileClicked();
			return true;
		} else if (itemId == MENU_IMPORT_PROFILE) {
			startImportConfig();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void startImportConfig() {
		Intent intent = new Intent(getActivity(),FileSelect.class);
		intent.putExtra(FileSelect.NO_INLINE_SELECTION, true);
		intent.putExtra(FileSelect.WINDOW_TITLE, R.string.import_configuration_file);
		startActivityForResult(intent, SELECT_PROFILE);
	}





	private void onAddProfileClicked() {
		Context context = getActivity();
		if (context != null) {
			final EditText entry = new EditText(context);
			entry.setSingleLine();

			AlertDialog.Builder dialog = new AlertDialog.Builder(context);
			dialog.setTitle(R.string.menu_add_profile);
			dialog.setMessage(R.string.add_profile_name_prompt);
			dialog.setView(entry);


			dialog.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = entry.getText().toString();
					if (getPM().getProfileByName(name)==null) {
						VpnProfile profile = new VpnProfile(name);
						addProfile(profile);
						editVPN(profile);
					} else {
						Toast.makeText(getActivity(), R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
					}
				}


			});
			dialog.setNegativeButton(android.R.string.cancel, null);
			dialog.create().show();
		}

	}


	private void addProfile(VpnProfile profile) {
		getPM().addProfile(profile);
		getPM().saveProfileList(getActivity());
		getPM().saveProfile(getActivity(),profile);
		mArrayadapter.add(profile);
	}





	private ProfileManager getPM() {
		return ProfileManager.getInstance(getActivity());
	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(resultCode == RESULT_VPN_DELETED){
			if(mArrayadapter != null && mEditProfile !=null)
				mArrayadapter.remove(mEditProfile);
		}

		if(resultCode != Activity.RESULT_OK)
			return;

		if (requestCode == START_VPN_CONFIG) {
			String configuredVPN = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);

			VpnProfile profile = ProfileManager.get(configuredVPN);
			getPM().saveProfile(getActivity(), profile);
			// Name could be modified, reset List adapter
			setListAdapter();

		} else if(requestCode== SELECT_PROFILE) {
			String filedata = data.getStringExtra(FileSelect.RESULT_DATA);
			Intent startImport = new Intent(getActivity(),ConfigConverter.class);
			startImport.setAction(ConfigConverter.IMPORT_PROFILE);
			Uri uri = new Uri.Builder().path(filedata).scheme("file").build();
			startImport.setData(uri);
			startActivityForResult(startImport, IMPORT_PROFILE);
		} else if(requestCode == IMPORT_PROFILE) {
			String profileUUID = data.getStringExtra(VpnProfile.EXTRA_PROFILEUUID);
			mArrayadapter.add(ProfileManager.get(profileUUID));
		}

	}


	private void editVPN(VpnProfile profile) {
		mEditProfile =profile;
		Intent vprefintent = new Intent(getActivity(),VPNPreferences.class)
		.putExtra(getActivity().getPackageName() + ".profileUUID", profile.getUUID().toString());

		startActivityForResult(vprefintent,START_VPN_CONFIG);
	}

	private void startVPN(VpnProfile profile) {

		getPM().saveProfile(getActivity(), profile);

		Intent intent = new Intent(getActivity(),LaunchVPN.class);
		intent.putExtra(LaunchVPN.EXTRA_KEY, profile.getUUID().toString());
		intent.setAction(Intent.ACTION_MAIN);
		startActivity(intent);
	}
}
