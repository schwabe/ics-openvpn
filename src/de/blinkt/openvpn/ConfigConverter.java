package de.blinkt.openvpn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import de.blinkt.openvpn.ConfigParser.ConfigParseError;

public class ConfigConverter extends ListActivity {

	public static final String IMPORT_PROFILE = "de.blinkt.openvpn.IMPORT_PROFILE";

	private VpnProfile mResult;
	private ArrayAdapter<String> mArrayAdapter;

	private List<String> mPathsegments;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId()==R.id.cancel){
			setResult(Activity.RESULT_CANCELED);
			finish();
		} else if(item.getItemId()==R.id.ok) {
			if(mResult==null) {
				log("Importing the config had error, cannot save it");
				return true;
			}
			
			Intent result = new Intent();
			ProfileManager vpl = ProfileManager.getInstance(this);
			
			setUniqueProfileName(vpl);
			vpl.addProfile(mResult);
			vpl.saveProfile(this, mResult);
			vpl.saveProfileList(this);
			result.putExtra(VpnProfile.EXTRA_PROFILEUUID,mResult.getUUID().toString());
			setResult(Activity.RESULT_OK, result);
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);

	}


	private void setUniqueProfileName(ProfileManager vpl) {
		int i=1;
		String newname = getString(R.string.converted_profile);
		
		while(vpl.getProfileByName(newname)!=null) {
			i++;
			newname = getString(R.string.converted_profile_i,i);
		}
		
		mResult.mName=newname;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.import_menu, menu);
		return true;
	}


	private String embedFile(String filename)
	{
		if(filename == null || filename.equals(""))
			return null;
		// Already embedded, nothing to do
		if(filename.startsWith(VpnProfile.INLINE_TAG))
			return filename;

		// Try diffent path relative to /mnt/sdcard
		File sdcard = Environment.getExternalStorageDirectory();
		File root = new File("/");
		
		Vector<File> dirlist = new Vector<File>();

		for(int i=mPathsegments.size()-1;i >=0 ;i--){
			String path = "";
			for (int j = 0;j<=i;j++) {
				path += "/" + mPathsegments.get(j);
			}
			dirlist.add(new File(path));
		}
		dirlist.add(sdcard);
		dirlist.add(root);
		
		
		String[] fileparts = filename.split("/");
		for(File rootdir:dirlist){
			String suffix="";
			for(int i=fileparts.length-1; i >=0;i--) {
				if(i==fileparts.length-1)
					suffix = fileparts[i];
				else
					suffix = fileparts[i] + "/" + suffix;

				File possibleFile = new File(rootdir,suffix);
				if(!possibleFile.canRead())
					continue;

				// read the file inline
				String filedata = VpnProfile.INLINE_TAG;
				byte[] buf =new byte[2048];

				log(R.string.trying_to_read, possibleFile.getAbsolutePath());
				try {
					FileInputStream fis = new FileInputStream(possibleFile);
					int len = fis.read(buf);
					while( len > 0){
						filedata += new String(buf,0,len);
						len = fis.read(buf);
					}
					fis.close();
					return filedata;
				} catch (FileNotFoundException e) {
					log(e.getLocalizedMessage());
				} catch (IOException e) {
					log(e.getLocalizedMessage());
				}	


			}
		}
		log(R.string.import_could_not_open,filename);
		return null;
	}

	void embedFiles() {
		// This where I would like to have a c++ style
		// void embedFile(std::string & option)

		mResult.mCaFilename = embedFile(mResult.mCaFilename);
		mResult.mClientCertFilename = embedFile(mResult.mClientCertFilename);
		mResult.mClientKeyFilename = embedFile(mResult.mClientKeyFilename);
		mResult.mTLSAuthFilename = embedFile(mResult.mTLSAuthFilename);
	}


	@Override
	protected void onStart() {
		super.onStart();

		mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		getListView().setAdapter(mArrayAdapter);
		final android.content.Intent intent = getIntent ();

		if (intent != null)
		{
			final android.net.Uri data = intent.getData ();
			if (data != null)
			{
				log(R.string.import_experimental);
				log(R.string.importing_config,data.toString());
				try {
					InputStream is = getContentResolver().openInputStream(data);
					mPathsegments = data.getPathSegments();
					
					doImport(is);
				} catch (FileNotFoundException e) {
					log(R.string.import_content_resolve_error);
				}
			} 
		} 

		return;
	}

	private void log(String logmessage) {
		mArrayAdapter.add(logmessage);
	}

	private void doImport(InputStream is) {
		ConfigParser cp = new ConfigParser();
		try {
			cp.parseConfig(is);
			VpnProfile vp = cp.convertProfile();
			mResult = vp;
			embedFiles();
			displayWarnings();
			log(R.string.import_done);
			return;

		} catch (IOException e) {
			log(R.string.error_reading_config_file);
			log(e.getLocalizedMessage());
		} catch (ConfigParseError e) {
			log(R.string.error_reading_config_file);
			log(e.getLocalizedMessage());			
		}
		mResult=null;

	}

	private void displayWarnings() {
		if(mResult.mUseCustomConfig) {
			log(R.string.import_warning_custom_options);
			log(mResult.mCustomConfigOptions);
		}

		if(mResult.mAuthenticationType==VpnProfile.TYPE_KEYSTORE) {
			log(R.string.import_pkcs12_to_keystore);
		}

	}


	private void log(int ressourceId, Object... formatArgs) {
		log(getString(ressourceId,formatArgs));
	}
}
