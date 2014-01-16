package de.blinkt.openvpn.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.Base64;
import android.webkit.MimeTypeMap;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.FileSelect;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.X509Utils;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.*;
import java.util.TreeSet;
import java.util.Vector;

import static android.os.Build.*;


public class FileSelectLayout extends LinearLayout implements OnClickListener {

    public void parseResponse(Intent data, Context c) {
        if (VERSION.SDK_INT < VERSION_CODES.KITKAT) {
            String fileData = data.getStringExtra(FileSelect.RESULT_DATA);
            setData(fileData, c);
        } else if (data != null) {
            Uri uri = data.getData();
            try {
                byte[] fileData = readBytesFromStream(c.getContentResolver().openInputStream(uri));
                String newData = null;
                switch (fileType) {
                    case CERTIFICATE:
                    case KEYFILE:
                        newData = new String(fileData, "UTF-8");
                        break;
                    case PKCS12:
                        newData = Base64.encodeToString(fileData, Base64.DEFAULT);
                        break;
                }
                setData(VpnProfile.INLINE_TAG + newData, c);

            } catch (IOException e) {
                VpnStatus.logException(e);
            }


        }
    }

    static private byte[] readBytesFromStream(InputStream input) throws IOException {

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        input.close();
        return buffer.toByteArray();
    }

    public enum FileType {
        PKCS12,
        CERTIFICATE,
        KEYFILE
    }

    private final boolean mIsCertificate;
    private TextView mDataView;
    private String mData;
    private Fragment mFragment;
    private int mTaskId;
    private Button mSelectButton;
    private FileType fileType;
    private String mTitle;
    private boolean mShowClear;
    private TextView mDataDetails;

    public FileSelectLayout(Context context, AttributeSet attrset) {
        super(context, attrset);
        inflate(getContext(), R.layout.file_select, this);

        TypedArray ta = context.obtainStyledAttributes(attrset, R.styleable.FileSelectLayout);

        mTitle = ta.getString(R.styleable.FileSelectLayout_title);
        mIsCertificate = ta.getBoolean(R.styleable.FileSelectLayout_certificate, true);

        TextView tview = (TextView) findViewById(R.id.file_title);
        tview.setText(mTitle);

        mDataView = (TextView) findViewById(R.id.file_selected_item);
        mDataDetails = (TextView) findViewById(R.id.file_selected_description);
        mSelectButton = (Button) findViewById(R.id.file_select_button);
        mSelectButton.setOnClickListener(this);

        ta.recycle();
    }

    public void setFragment(Fragment fragment, int i, FileType ft) {
        mTaskId = i;
        mFragment = fragment;
        fileType = ft;
    }

    public void getCertificateFileDialog() {
        Intent startFC = new Intent(getContext(), FileSelect.class);
        startFC.putExtra(FileSelect.START_DATA, mData);
        startFC.putExtra(FileSelect.WINDOW_TITLE, mTitle);
        if (fileType == FileType.PKCS12)
            startFC.putExtra(FileSelect.DO_BASE64_ENCODE, true);
        if (mShowClear)
            startFC.putExtra(FileSelect.SHOW_CLEAR_BUTTON, true);

        mFragment.startActivityForResult(startFC, mTaskId);
    }


    public String getData() {
        return mData;
    }

    public void setData(String data, Context c) {
        mData = data;
        if (data == null) {
            mDataView.setText(mFragment.getString(R.string.no_data));
            mDataDetails.setText("");
        } else {
            if (mData.startsWith(VpnProfile.INLINE_TAG))
                mDataView.setText(R.string.inline_file_data);
            else
                mDataView.setText(data);
            if (mIsCertificate)
                mDataDetails.setText(X509Utils.getCertificateFriendlyName(c, data));
        }

    }

    @Override
    public void onClick(View v) {
        if (v == mSelectButton) {
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT)
                startFilePicker();
            else
                getCertificateFileDialog();
        }
    }


    @TargetApi(VERSION_CODES.KITKAT)
    private void startFilePicker() {


        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        TreeSet<String> supportedMimeTypes = new TreeSet<String>();
        Vector<String> extensions = new Vector<String>();

        switch (fileType) {
            case PKCS12:
                i.setType("application/x-pkcs12");
                supportedMimeTypes.add("application/x-pkcs12");
                extensions.add("p12");
                extensions.add("pfx");
                break;
            case CERTIFICATE:
                i.setType("application/x-pem-file");
                supportedMimeTypes.add("application/x-x509-ca-cert");
                supportedMimeTypes.add("application/x-x509-user-cert");
                supportedMimeTypes.add("application/x-pem-file");
                supportedMimeTypes.add("text/plain");

                extensions.add("pem");
                extensions.add("crt");
                break;
            case KEYFILE:
                i.setType("application/x-pem-file");
                supportedMimeTypes.add("application/x-pem-file");
                supportedMimeTypes.add("application/pkcs8");
                extensions.add("key");
                break;
        }

        MimeTypeMap mtm = MimeTypeMap.getSingleton();

        for (String ext : new String[]{"ovpn", "conf"}) {
            String mimeType = mtm.getMimeTypeFromExtension(ext);
            if (mimeType != null)
                supportedMimeTypes.add(mimeType);
            else
                supportedMimeTypes.add("application/octet-stream");
        }


        i.putExtra(Intent.EXTRA_MIME_TYPES, supportedMimeTypes.toArray(new String[supportedMimeTypes.size()]));
        mFragment.startActivityForResult(i, mTaskId);
    }

    public void setShowClear() {
        mShowClear = true;
    }

}
