
package de.blinkt.openvpn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import de.blinkt.openvpn.ConfigParser.ConfigParseError;

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

		if(((CheckBox)findViewById(R.id.correcttls)).isChecked() && isOldCNFormat()) {
			convertTLSRemote();
		}
		
		setUniqueProfileName(vpl);
		vpl.addProfile(mResult);
		vpl.saveProfile(this, mResult);
		vpl.saveProfileList(this);
		result.putExtra(VpnProfile.EXTRA_PROFILEUUID,mResult.getUUID().toString());
		setResult(Activity.RESULT_OK, result);
		finish();
	}



	private void convertTLSRemote() {
		if(mResult.mRemoteCN.startsWith("/"))
			mResult.mRemoteCN = mResult.mRemoteCN.substring(1);
		mResult.mRemoteCN = mResult.mRemoteCN.replace("/", ", ");
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
		if(!((CheckBox)findViewById(R.id.importpkcs12)).isChecked())
			return null;
		File possiblepkcs12 = findFile(mResult.mPKCS12Filename);

		if(possiblepkcs12!=null) {
			Intent inkeyintent = KeyChain.createInstallIntent();
			byte[] pkcs12data;
			try {
				pkcs12data = readBytesFromFile(possiblepkcs12);
			} catch (IOException e) {
				return null;
			}

			inkeyintent.putExtra(KeyChain.EXTRA_PKCS12,pkcs12data );

			mAliasName = possiblepkcs12.getName().replace(".p12", "");
			if(mAliasName.equals(""))
				mAliasName=null;

			if(mAliasName!=null){
				inkeyintent.putExtra(KeyChain.EXTRA_NAME, mAliasName);
			}
			return inkeyintent;

		}
		return null;
	}


	private void setUniqueProfileName(ProfileManager vpl) {
		int i=0;

		String newname = mPossibleName;

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

	private String embedFile(String filename)
	{
		if(filename==null)
			return null;

		// Already embedded, nothing to do
		if(filename.startsWith(VpnProfile.INLINE_TAG))
			return filename;

		File possibleFile = findFile(filename);
		if(possibleFile==null)
			return null;
		else
			return readFileContent(possibleFile);

	}

	private File findFile(String filename)
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
		log(R.string.import_could_not_open,filename);
		return null;
	}

	String readFileContent(File possibleFile) {
		String filedata =  "";
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
			return VpnProfile.INLINE_TAG + filedata;
		} catch (FileNotFoundException e) {
			log(e.getLocalizedMessage());
		} catch (IOException e) {
			log(e.getLocalizedMessage());
		}	

		return null;
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

		mResult.mCaFilename = embedFile(mResult.mCaFilename);
		mResult.mClientCertFilename = embedFile(mResult.mClientCertFilename);
		mResult.mClientKeyFilename = embedFile(mResult.mClientKeyFilename);
		mResult.mTLSAuthFilename = embedFile(mResult.mTLSAuthFilename);

		if(mResult.mUsername != null && !mResult.mUsername.equals("")){
			String data =embedFile(mResult.mUsername);
			mResult.mName=null;
			if(data!=null) {
				data = data.replace(VpnProfile.INLINE_TAG, "");
				String[] parts = data.split("\n");
				if(parts.length >= 2) {
					mResult.mName=parts[0];
					mResult.mPassword=parts[1];
				}
			}
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

		if (isOldCNFormat())
			findViewById(R.id.correcttls).setVisibility(View.VISIBLE);
	}

	private boolean isOldCNFormat() {
		return mResult.mCheckRemoteCN && mResult.mRemoteCN.contains("/") && ! mResult.mRemoteCN.contains("_");
	}

	private void log(int ressourceId, Object... formatArgs) {
		log(getString(ressourceId,formatArgs));
	}
}
