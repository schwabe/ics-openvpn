package de.blinkt.openvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class VPNProfileList extends ListFragment {
	class VPNArrayAdapter extends ArrayAdapter<VpnProfile> {

		public VPNArrayAdapter(Context context, int resource,
				int textViewResourceId) {
			super(context, resource, textViewResourceId);

		}
		
	}
	
	
	
	private static final int MENU_ADD_PROFILE = Menu.FIRST;

	private static final int START_VPN_CONFIG = 92;

	private ArrayAdapter<VpnProfile> mArrayadapter;

	protected Object mActionMode;

	protected VpnProfile mEditProfile=null;

	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		// Debug load JNI
		//OpenVPN.foo();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		ListView lv = getListView();
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			// Called when the user long-clicks on someView

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
		        if (mActionMode != null) {
		            return false;
		        }

		        // Start the CAB using the ActionMode.Callback defined above
		        mActionMode = getActivity().startActionMode(mActionModeCallback);
				mEditProfile =(VpnProfile) getListAdapter().getItem(position);

				//getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		        //getListView().setSelection(position);
		        return true;
			}
		});
		
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				VpnProfile profile =(VpnProfile) getListAdapter().getItem(position);
				startVPN(profile);
			}
		});
		

//		mArrayadapter = new ArrayAdapter<VpnProfile>(getActivity(),R.layout.vpn_list_item,R.id.vpn_item_title);
		mArrayadapter = new ArrayAdapter<VpnProfile>(getActivity(),android.R.layout.simple_list_item_activated_1);
		mArrayadapter.addAll(getPM().getProfiles());
		
		setListAdapter(mArrayadapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(0, MENU_ADD_PROFILE, 0, R.string.menu_add_profile)
		.setIcon(android.R.drawable.ic_menu_add)
		.setAlphabeticShortcut('a')
		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
				| MenuItem.SHOW_AS_ACTION_WITH_TEXT);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		final int itemId = item.getItemId();
		if (itemId == MENU_ADD_PROFILE) {
			onAddProfileClicked();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void askProfileRemoval() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		dialog.setTitle("Confirm deletion");
		dialog.setMessage(getString(R.string.remove_vpn_query, mEditProfile.mName));

		dialog.setPositiveButton(android.R.string.yes,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeProfile(mEditProfile);
			}

		});
		dialog.setNegativeButton(android.R.string.no,null);
		dialog.create().show();
	}

	protected void removeProfile(VpnProfile profile) {
		mArrayadapter.remove(profile);
		getPM().removeProfile(getActivity(),profile);
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
					} else {
						Toast.makeText(getActivity(), R.string.duplicate_profile_name, Toast.LENGTH_LONG).show();
					}
				}


			});
			dialog.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
				}
			});
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
		if (requestCode == START_VPN_CONFIG && resultCode == Activity.RESULT_OK) {
			String configuredVPN = data.getStringExtra(getActivity().getPackageName() + ".profileUUID");

			VpnProfile profile = ProfileManager.get(configuredVPN);
			getPM().saveProfile(getActivity(), profile);
			// Name could be modified

		}

	}


	private void editVPN(VpnProfile profile) {

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
		
		getActivity().finish();
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.vpn_context, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;// Return false if nothing is done
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		      switch (item.getItemId()) {
	            case R.id.remove_vpn:
	                askProfileRemoval();
	                mode.finish(); // Action picked, so close the CAB
	                return true;
/*	            case R.id.connect_vpn:
	            	startVPN(mEditProfile);
	            	mode.finish();
	            	return true; */ 
	            case R.id.edit_vpn:
	            	editVPN(mEditProfile);
	            	mode.finish();
	            	return true;
	            default:
	                return false;
	        }		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
	        mActionMode = null;
	        getListView().setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
	};

}
