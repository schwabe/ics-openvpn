
package de.blinkt.openvpn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError;
import de.blinkt.openvpn.core.ProfileManager;

public class ConfigConverter extends ListActivity {

	public static final String IMPORT_PROFILE = "de.blinkt.openvpn.IMPORT_PROFILE";

	private VpnProfile mResult;
	private ArrayAdapter<String> mArrayAdapter;

	private List<String> mPathsegments;

	private String mAliasName=null;

	private int RESULT_INSTALLPKCS12 = 7;

	private String mPossibleName=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_converter);
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

			Intent in = installPKCS12();

			if(in != null)
				startActivityForResult(in, RESULT_INSTALLPKCS12);
			else
				saveProfile();

			return true;
		}

		return super.onOptionsItemSelected(item);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode==RESULT_INSTALLPKCS12) {
			if(resultCode==Activity.RESULT_OK) {
				showCertDialog();
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	private void saveProfile() {
		Intent result = new Intent();
		ProfileManager vpl = ProfileManager.getInstance(this);

		setUniqueProfileName(vpl);
		vpl.addProfile(mResult);
		vpl.saveProfile(this, mResult);
		vpl.saveProfileList(this);
		result.putExtra(VpnProfile.EXTRA_PROFILEUUID,mResult.getUUID().toString());
		setResult(Activity.RESULT_OK, result);
		finish();
	}

	public void showCertDialog () {
		try	{
			KeyChain.choosePrivateKeyAlias(this,
					new KeyChainAliasCallback() {

				public void alias(String alias) {
					// Credential alias selected.  Remember the alias selection for future use.
					mResult.mAlias=alias;
					saveProfile();
				}


			},
			new String[] {"RSA"}, // List of acceptable key types. null for any
			null,                        // issuer, null for any
			mResult.mServerName,      // host name of server requesting the cert, null if unavailable
			-1,                         // port of server requesting the cert, -1 if unavailable
			mAliasName);                       // alias to preselect, null if unavailable
		} catch (ActivityNotFoundException anf) {
			Builder ab = new AlertDialog.Builder(this);
			ab.setTitle(R.string.broken_image_cert_title);
			ab.setMessage(R.string.broken_image_cert);
			ab.setPositiveButton(android.R.string.ok, null);
			ab.show();
		}
	}


	private Intent installPKCS12() {
		
		if(!((CheckBox)findViewById(R.id.importpkcs12)).isChecked()) {
			setAuthTypeToEmbeddedPKCS12();
			return null;
			
		}
		String pkcs12datastr = mResult.mPKCS12Filename;
		if(pkcs12datastr!=null && pkcs12datastr.startsWith(VpnProfile.INLINE_TAG)) {
			Intent inkeyintent = KeyChain.createInstallIntent();
			
			pkcs12datastr= pkcs12datastr.substring(VpnProfile.INLINE_TAG.length());
			
			
			byte[] pkcs12data = Base64.decode(pkcs12datastr, Base64.DEFAULT);


			inkeyintent.putExtra(KeyChain.EXTRA_PKCS12,pkcs12data );

			if(mAliasName.equals(""))
				mAliasName=null;

			if(mAliasName!=null){
				inkeyintent.putExtra(KeyChain.EXTRA_NAME, mAliasName);
			}
			return inkeyintent;

		}
		return null;
	}



	private void setAuthTypeToEmbeddedPKCS12() {
		if(mResult.mPKCS12Filename!=null && mResult.mPKCS12Filename.startsWith(VpnProfile.INLINE_TAG)) {
			if(mResult.mAuthenticationType==VpnProfile.TYPE_USERPASS_KEYSTORE)
				mResult.mAuthenticationType=VpnProfile.TYPE_USERPASS_PKCS12;
			
			if(mResult.mAuthenticationType==VpnProfile.TYPE_KEYSTORE)
				mResult.mAuthenticationType=VpnProfile.TYPE_PKCS12;
			
		}
	}





	private void setUniqueProfileName(ProfileManager vpl) {
		int i=0;

		String newname = mPossibleName;

		// 	Default to 
		if(mResult.mName!=null && !ConfigParser.CONVERTED_PROFILE.equals(mResult.mName))
			newname=mResult.mName;
			
		while(vpl.getProfileByName(newname)!=null) {
			i++;
			if(i==1)
				newname = getString(R.string.converted_profile);
			else
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
	
	private String embedFile(String filename) {
		return embedFile(filename, false);		
	}

	private String embedFile(String filename, boolean base64encode)
	{
		if(filename==null)
			return null;

		// Already embedded, nothing to do
		if(filename.startsWith(VpnProfile.INLINE_TAG))
			return filename;

		File possibleFile = findFile(filename);
		if(possibleFile==null)
			return filename;
		else
			return readFileContent(possibleFile,base64encode);

	}

	private File findFile(String filename) {
		File foundfile =findFileRaw(filename);
		
		if (foundfile==null && filename!=null && !filename.equals(""))
			log(R.string.import_could_not_open,filename);

		return foundfile;
	}



	private File findFileRaw(String filename)
	{
		if(filename == null || filename.equals(""))
			return null;

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
				return possibleFile;

			}
		}
		return null;
	}

	String readFileContent(File possibleFile, boolean base64encode) {
		byte [] filedata;
		try {
			filedata = readBytesFromFile(possibleFile);
		} catch (IOException e) {
			log(e.getLocalizedMessage());
			return null;
		}
		
		String data;
		if(base64encode) {
			data = Base64.encodeToString(filedata, Base64.DEFAULT);
		} else {
			data = new String(filedata);

		}
		return VpnProfile.INLINE_TAG + data;
		
	}


	private byte[] readBytesFromFile(File file) throws IOException {
		InputStream input = new FileInputStream(file);

		long len= file.length();


		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) len];

		// Read in the bytes
		int offset = 0;
		int bytesRead = 0;
		while (offset < bytes.length
				&& (bytesRead=input.read(bytes, offset, bytes.length-offset)) >= 0) {
			offset += bytesRead;
		}

		input.close();
		return bytes;
	}

	void embedFiles() {
		// This where I would like to have a c++ style
		// void embedFile(std::string & option)

		if (mResult.mPKCS12Filename!=null) {
			File pkcs12file = findFileRaw(mResult.mPKCS12Filename);
			if(pkcs12file!=null) {
				mAliasName = pkcs12file.getName().replace(".p12", "");
			} else {
				mAliasName = "Imported PKCS12";
			}
		}
			
		
		mResult.mCaFilename = embedFile(mResult.mCaFilename);
		mResult.mClientCertFilename = embedFile(mResult.mClientCertFilename);
		mResult.mClientKeyFilename = embedFile(mResult.mClientKeyFilename);
		mResult.mTLSAuthFilename = embedFile(mResult.mTLSAuthFilename);
		mResult.mPKCS12Filename = embedFile(mResult.mPKCS12Filename,true);
		

		if(mResult.mUsername == null && mResult.mPassword != null ){
			String data =embedFile(mResult.mPassword);
			ConfigParser.useEmbbedUserAuth(mResult, data);			
		}
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
				//log(R.string.import_experimental);
				log(R.string.importing_config,data.toString());
				try {
					if(data.getScheme().equals("file")) {
						mPossibleName = data.getLastPathSegment();
						if(mPossibleName!=null){
							mPossibleName =mPossibleName.replace(".ovpn", "");
							mPossibleName =mPossibleName.replace(".conf", "");
						}
					}
					InputStream is = getContentResolver().openInputStream(data);
					mPathsegments = data.getPathSegments();

					doImport(is);
				} catch (FileNotFoundException e) {
					log(R.string.import_content_resolve_error);
				}
			} 
		} 
	}

	private void log(String logmessage) {
		mArrayAdapter.add(logmessage);
	}

	private void doImport(InputStream is) {
		ConfigParser cp = new ConfigParser();
		try {
			InputStreamReader isr = new InputStreamReader(is);

			cp.parseConfig(isr);
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
			String copt = mResult.mCustomConfigOptions;
			if(copt.startsWith("#")) {
				int until = copt.indexOf('\n');
				copt = copt.substring(until+1);
			}

			log(copt);
		}

		if(mResult.mAuthenticationType==VpnProfile.TYPE_KEYSTORE ||
				mResult.mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
			findViewById(R.id.importpkcs12).setVisibility(View.VISIBLE);
		}

	}

	private void log(int ressourceId, Object... formatArgs) {
		log(getString(ressourceId,formatArgs));
	}
}
