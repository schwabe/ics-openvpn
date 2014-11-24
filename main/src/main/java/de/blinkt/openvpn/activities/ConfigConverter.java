
/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import junit.framework.Assert;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.ConfigParser;
import de.blinkt.openvpn.core.ConfigParser.ConfigParseError;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.fragments.Utils;
import de.blinkt.openvpn.views.FileSelectLayout;

import static de.blinkt.openvpn.views.FileSelectLayout.FileSelectCallback;

public class ConfigConverter extends Activity implements FileSelectCallback {

    public static final String IMPORT_PROFILE = "de.blinkt.openvpn.IMPORT_PROFILE";
    private static final int RESULT_INSTALLPKCS12 = 7;
    private static final int CHOOSE_FILE_OFFSET = 1000;
    public static final String VPNPROFILE = "vpnProfile";

    private VpnProfile mResult;
    
    private transient List<String> mPathsegments;

    private String mAliasName = null;


    private Map<Utils.FileType, FileSelectLayout> fileSelectMap = new HashMap<Utils.FileType, FileSelectLayout>();
    private String mEmbeddedPwFile;
    private Vector<String> mLogEntries = new Vector<String>();

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.cancel) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        } else if (item.getItemId() == R.id.ok) {
            if (mResult == null) {
                log("Importing the config had error, cannot save it");
                return true;
            }

            Intent in = installPKCS12();

            if (in != null)
                startActivityForResult(in, RESULT_INSTALLPKCS12);
            else
                saveProfile();

            return true;
        }

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mResult != null)
            outState.putSerializable(VPNPROFILE, mResult);
        outState.putString("mAliasName", mAliasName);



        String[] logentries = mLogEntries.toArray(new String[mLogEntries.size()]);

        outState.putStringArray("logentries", logentries);

        int[] fileselects = new int[fileSelectMap.size()];
        int k = 0;
        for (Utils.FileType key : fileSelectMap.keySet()) {
            fileselects[k] = key.getValue();
            k++;
        }
        outState.putIntArray("fileselects", fileselects);
        outState.putString("pwfile",mEmbeddedPwFile);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == RESULT_INSTALLPKCS12 && resultCode == Activity.RESULT_OK) {
            showCertDialog();
        }

        if (resultCode == Activity.RESULT_OK && requestCode >= CHOOSE_FILE_OFFSET) {
            Utils.FileType type = Utils.FileType.getFileTypeByValue(requestCode - CHOOSE_FILE_OFFSET);


            FileSelectLayout fs = fileSelectMap.get(type);
            fs.parseResponse(result, this);

            String data = fs.getData();

            switch (type) {
                case USERPW_FILE:
                    mEmbeddedPwFile = data;
                    break;
                case PKCS12:
                    mResult.mPKCS12Filename = data;
                    break;
                case TLS_AUTH_FILE:
                    mResult.mTLSAuthFilename = data;
                    break;
                case CA_CERTIFICATE:
                    mResult.mCaFilename = data;
                    break;
                case CLIENT_CERTIFICATE:
                    mResult.mClientCertFilename = data;
                    break;
                case KEYFILE:
                    mResult.mClientKeyFilename = data;
                    break;
                default:
                    Assert.fail();
            }
        }

        super.onActivityResult(requestCode, resultCode, result);
    }

    private void saveProfile() {
        Intent result = new Intent();
        ProfileManager vpl = ProfileManager.getInstance(this);

        if (!TextUtils.isEmpty(mEmbeddedPwFile))
            ConfigParser.useEmbbedUserAuth(mResult, mEmbeddedPwFile);

        vpl.addProfile(mResult);
        vpl.saveProfile(this, mResult);
        vpl.saveProfileList(this);
        result.putExtra(VpnProfile.EXTRA_PROFILEUUID, mResult.getUUID().toString());
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    public void showCertDialog() {
        try {
            KeyChain.choosePrivateKeyAlias(this,
                    new KeyChainAliasCallback() {

                        public void alias(String alias) {
                            // Credential alias selected.  Remember the alias selection for future use.
                            mResult.mAlias = alias;
                            saveProfile();
                        }


                    },
                    new String[]{"RSA"}, // List of acceptable key types. null for any
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

        if (!((CheckBox) findViewById(R.id.importpkcs12)).isChecked()) {
            setAuthTypeToEmbeddedPKCS12();
            return null;

        }
        String pkcs12datastr = mResult.mPKCS12Filename;
        if (VpnProfile.isEmbedded(pkcs12datastr)) {
            Intent inkeyIntent = KeyChain.createInstallIntent();

            pkcs12datastr = VpnProfile.getEmbeddedContent(pkcs12datastr);


            byte[] pkcs12data = Base64.decode(pkcs12datastr, Base64.DEFAULT);


            inkeyIntent.putExtra(KeyChain.EXTRA_PKCS12, pkcs12data);

            if (mAliasName.equals(""))
                mAliasName = null;

            if (mAliasName != null) {
                inkeyIntent.putExtra(KeyChain.EXTRA_NAME, mAliasName);
            }
            return inkeyIntent;

        }
        return null;
    }


    private void setAuthTypeToEmbeddedPKCS12() {
        if (VpnProfile.isEmbedded(mResult.mPKCS12Filename)) {
            if (mResult.mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE)
                mResult.mAuthenticationType = VpnProfile.TYPE_USERPASS_PKCS12;

            if (mResult.mAuthenticationType == VpnProfile.TYPE_KEYSTORE)
                mResult.mAuthenticationType = VpnProfile.TYPE_PKCS12;

        }
    }


    private String getUniqueProfileName(String possibleName) {

        int i = 0;

        ProfileManager vpl = ProfileManager.getInstance(this);

        String newname = possibleName;

        // 	Default to
        if (mResult.mName != null && !ConfigParser.CONVERTED_PROFILE.equals(mResult.mName))
            newname = mResult.mName;

        while (newname == null || vpl.getProfileByName(newname) != null) {
            i++;
            if (i == 1)
                newname = getString(R.string.converted_profile);
            else
                newname = getString(R.string.converted_profile_i, i);
        }

        return newname;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.import_menu, menu);
        return true;
    }

    private String embedFile(String filename, Utils.FileType type) {
        if (filename == null)
            return null;

        // Already embedded, nothing to do
        if (VpnProfile.isEmbedded(filename))
            return filename;

        File possibleFile = findFile(filename, type);
        if (possibleFile == null)
            return filename;
        else
            return readFileContent(possibleFile, type == Utils.FileType.PKCS12);

    }

    private File findFile(String filename, Utils.FileType fileType) {
        File foundfile = findFileRaw(filename);

        if (foundfile == null && filename != null && !filename.equals("")) {
            log(R.string.import_could_not_open, filename);
            addFileSelectDialog(fileType);
        }


        return foundfile;
    }

    private void addFileSelectDialog(Utils.FileType type) {
        int titleRes = 0;
        String value = null;
        switch (type) {
            case KEYFILE:
                titleRes = R.string.client_key_title;
                if (mResult != null)
                    value = mResult.mClientKeyFilename;
                break;
            case CLIENT_CERTIFICATE:
                titleRes = R.string.client_certificate_title;
                if (mResult != null)
                    value = mResult.mClientCertFilename;
                break;
            case CA_CERTIFICATE:
                titleRes = R.string.ca_title;
                if (mResult != null)
                    value = mResult.mCaFilename;
                break;
            case TLS_AUTH_FILE:
                titleRes = R.string.tls_auth_file;
                if (mResult != null)
                    value = mResult.mTLSAuthFilename;
                break;
            case PKCS12:
                titleRes = R.string.client_pkcs12_title;
                if (mResult != null)
                    value = mResult.mPKCS12Filename;
                break;

            case USERPW_FILE:
                titleRes = R.string.userpw_file;
                value = mEmbeddedPwFile;
                break;

        }

        boolean isCert = type == Utils.FileType.CA_CERTIFICATE || type == Utils.FileType.CLIENT_CERTIFICATE;
        FileSelectLayout fl = new FileSelectLayout(this, getString(titleRes), isCert, false);
        fileSelectMap.put(type, fl);
        fl.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ((LinearLayout) findViewById(R.id.config_convert_root)).addView(fl, 2);
        findViewById(R.id.files_missing_hint).setVisibility(View.VISIBLE);
        fl.setData(value, this);
        int i = getFileLayoutOffset(type);
        fl.setCaller(this, i, type);

    }

    private int getFileLayoutOffset(Utils.FileType type) {
        return CHOOSE_FILE_OFFSET + type.getValue();
    }


    private File findFileRaw(String filename) {
        if (filename == null || filename.equals(""))
            return null;

        // Try diffent path relative to /mnt/sdcard
        File sdcard = Environment.getExternalStorageDirectory();
        File root = new File("/");

        HashSet<File> dirlist = new HashSet<File>();

        for (int i = mPathsegments.size() - 1; i >= 0; i--) {
            String path = "";
            for (int j = 0; j <= i; j++) {
                path += "/" + mPathsegments.get(j);
            }
            // Do a little hackish dance for the Android File Importer
            // /document/primary:ovpn/openvpn-imt.conf


            if (path.indexOf(':') != -1 && path.lastIndexOf('/') > path.indexOf(':')) {
                String possibleDir = path.substring(path.indexOf(':') + 1, path.length());
                possibleDir = possibleDir.substring(0, possibleDir.lastIndexOf('/'));


                dirlist.add(new File(sdcard, possibleDir));

            }
            dirlist.add(new File(path));


        }
        dirlist.add(sdcard);
        dirlist.add(root);


        String[] fileparts = filename.split("/");
        for (File rootdir : dirlist) {
            String suffix = "";
            for (int i = fileparts.length - 1; i >= 0; i--) {
                if (i == fileparts.length - 1)
                    suffix = fileparts[i];
                else
                    suffix = fileparts[i] + "/" + suffix;

                File possibleFile = new File(rootdir, suffix);
                if (!possibleFile.canRead())
                    continue;

                // read the file inline
                return possibleFile;

            }
        }
        return null;
    }

    String readFileContent(File possibleFile, boolean base64encode) {
        byte[] filedata;
        try {
            filedata = readBytesFromFile(possibleFile);
        } catch (IOException e) {
            log(e.getLocalizedMessage());
            return null;
        }

        String data;
        if (base64encode) {
            data = Base64.encodeToString(filedata, Base64.DEFAULT);
        } else {
            data = new String(filedata);

        }

        return VpnProfile.DISPLAYNAME_TAG + possibleFile.getName() + VpnProfile.INLINE_TAG + data;

    }


    private byte[] readBytesFromFile(File file) throws IOException {
        InputStream input = new FileInputStream(file);

        long len = file.length();
        if (len > VpnProfile.MAX_EMBED_FILE_SIZE)
            throw new IOException("File size of file to import too large.");

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) len];

        // Read in the bytes
        int offset = 0;
        int bytesRead;
        while (offset < bytes.length
                && (bytesRead = input.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += bytesRead;
        }

        input.close();
        return bytes;
    }

    void embedFiles() {
        // This where I would like to have a c++ style
        // void embedFile(std::string & option)

        if (mResult.mPKCS12Filename != null) {
            File pkcs12file = findFileRaw(mResult.mPKCS12Filename);
            if (pkcs12file != null) {
                mAliasName = pkcs12file.getName().replace(".p12", "");
            } else {
                mAliasName = "Imported PKCS12";
            }
        }


        mResult.mCaFilename = embedFile(mResult.mCaFilename, Utils.FileType.CA_CERTIFICATE);
        mResult.mClientCertFilename = embedFile(mResult.mClientCertFilename, Utils.FileType.CLIENT_CERTIFICATE);
        mResult.mClientKeyFilename = embedFile(mResult.mClientKeyFilename, Utils.FileType.KEYFILE);
        mResult.mTLSAuthFilename = embedFile(mResult.mTLSAuthFilename, Utils.FileType.TLS_AUTH_FILE);
        mResult.mPKCS12Filename = embedFile(mResult.mPKCS12Filename, Utils.FileType.PKCS12);
        mEmbeddedPwFile = embedFile(mResult.mPassword, Utils.FileType.USERPW_FILE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.config_converter);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(VPNPROFILE)) {
            mResult = (VpnProfile) savedInstanceState.getSerializable(VPNPROFILE);
            mAliasName = savedInstanceState.getString("mAliasName");
            mEmbeddedPwFile = savedInstanceState.getString("pwfile");

            if (savedInstanceState.containsKey("logentries")) {
                //noinspection ConstantConditions
                for (String logItem : savedInstanceState.getStringArray("logentries"))
                    log(logItem);
            }
            if (savedInstanceState.containsKey("fileselects")) {
                //noinspection ConstantConditions
                for (int k : savedInstanceState.getIntArray("fileselects")) {
                    addFileSelectDialog(Utils.FileType.getFileTypeByValue(k));
                }
            }
            return;
        }


        final android.content.Intent intent = getIntent();

        if (intent != null) {
            final android.net.Uri data = intent.getData();
            if (data != null) {
                //log(R.string.import_experimental);
                log(R.string.importing_config, data.toString());
                try {
                    String possibleName = null;
                    if ((data.getScheme() != null && data.getScheme().equals("file")) ||
                            (data.getLastPathSegment() != null &&
                                    (data.getLastPathSegment().endsWith(".ovpn") ||
                                            data.getLastPathSegment().endsWith(".conf")))
                            ) {
                        possibleName = data.getLastPathSegment();
                        if (possibleName.lastIndexOf('/') != -1)
                            possibleName = possibleName.substring(possibleName.lastIndexOf('/') + 1);

                    }
                    InputStream is = getContentResolver().openInputStream(data);
                    mPathsegments = data.getPathSegments();

                    Cursor cursor = null;
                    if (data!=null)
                         cursor = getContentResolver().query(data, null, null, null, null);

                    try {

                        if (cursor!=null && cursor.moveToFirst()) {
                            int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                            if (columnIndex != -1) {
                                String displayName = cursor.getString(columnIndex);
                                if (displayName != null)
                                    possibleName = displayName;
                            }
                            columnIndex = cursor.getColumnIndex("mime_type");
                            if (columnIndex != -1) {
                                log("Opening Mime TYPE: " + cursor.getString(columnIndex));
                            }
                        }
                    } finally {
                        if(cursor!=null)
                            cursor.close();
                    }
                    if (possibleName != null) {
                        possibleName = possibleName.replace(".ovpn", "");
                        possibleName = possibleName.replace(".conf", "");
                    }

                    doImport(is, possibleName);

                } catch (FileNotFoundException e) {
                    log(R.string.import_content_resolve_error);
                } catch (SecurityException se) {
                    log(R.string.import_content_resolve_error + ":" + se.getLocalizedMessage());
                }
            }

            // We parsed the intent, relay on saved instance for restoring
            setIntent(null);
        }


    }


    @Override
    protected void onStart() {
        super.onStart();


    }

    private void log(String logmessage) {
        mLogEntries.add(logmessage);
        TextView tv = new TextView(this);
        tv.setText(logmessage);
        LinearLayout logLayout = (LinearLayout) findViewById(R.id.config_convert_root);
        logLayout.addView(tv);
    }

    private void doImport(InputStream is, String newName) {
        ConfigParser cp = new ConfigParser();
        try {
            InputStreamReader isr = new InputStreamReader(is);

            cp.parseConfig(isr);
            mResult = cp.convertProfile();
            embedFiles();
            displayWarnings();
            mResult.mName = getUniqueProfileName(newName);

            log(R.string.import_done);
            return;

        } catch (IOException e) {
            log(R.string.error_reading_config_file);
            log(e.getLocalizedMessage());
        } catch (ConfigParseError e) {
            log(R.string.error_reading_config_file);
            log(e.getLocalizedMessage());
        }
        mResult = null;

    }

    private void displayWarnings() {
        if (mResult.mUseCustomConfig) {
            log(R.string.import_warning_custom_options);
            String copt = mResult.mCustomConfigOptions;
            if (copt.startsWith("#")) {
                int until = copt.indexOf('\n');
                copt = copt.substring(until + 1);
            }

            log(copt);
        }

        if (mResult.mAuthenticationType == VpnProfile.TYPE_KEYSTORE ||
                mResult.mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
            findViewById(R.id.importpkcs12).setVisibility(View.VISIBLE);
        }

    }

    private void log(int ressourceId, Object... formatArgs) {
        log(getString(ressourceId, formatArgs));
    }


}
