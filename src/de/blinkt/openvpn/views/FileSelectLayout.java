package de.blinkt.openvpn.views;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.FileSelect;
import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.X509Utils;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.blinkt.openvpn.fragments.Utils;

import java.io.*;

import static android.os.Build.*;


public class FileSelectLayout extends LinearLayout implements OnClickListener {

    public void parseResponse(Intent data, Context c) {
        if (VERSION.SDK_INT < VERSION_CODES.KITKAT) {
            String fileData = data.getStringExtra(FileSelect.RESULT_DATA);
            setData(fileData, c);
        } else if (data != null) {
            try {
                String newData = Utils.getFilePickerResult(fileType, data, c);
                if (newData!=null)
                    setData(newData, c);

            } catch (IOException e) {
                VpnStatus.logException(e);
            }


        }
    }

    public interface FileSelectCallback {

        String getString(int res);

        void startActivityForResult(Intent startFC, int mTaskId);
    }

    private boolean mIsCertificate;
    private TextView mDataView;
    private String mData;
    private FileSelectCallback mFragment;
    private int mTaskId;
    private Button mSelectButton;
    private Utils.FileType fileType;
    private String mTitle;
    private boolean mShowClear;
    private TextView mDataDetails;

    public FileSelectLayout(Context context, AttributeSet attrset) {
        super(context, attrset);

        TypedArray ta = context.obtainStyledAttributes(attrset, R.styleable.FileSelectLayout);

        setupViews(ta.getString(R.styleable.FileSelectLayout_title), ta.getBoolean(R.styleable.FileSelectLayout_certificate, true));

        ta.recycle();
    }

    public FileSelectLayout (Context context, String title, boolean isCerticate)
    {
        super(context);

        setupViews(title, isCerticate);

    }

    private void setupViews(String title, boolean isCertificate) {
        inflate(getContext(), R.layout.file_select, this);

        mTitle = title;
        mIsCertificate = isCertificate;

        TextView tview = (TextView) findViewById(R.id.file_title);
        tview.setText(mTitle);

        mDataView = (TextView) findViewById(R.id.file_selected_item);
        mDataDetails = (TextView) findViewById(R.id.file_selected_description);
        mSelectButton = (Button) findViewById(R.id.file_select_button);
        mSelectButton.setOnClickListener(this);
    }


    public void setCaller(FileSelectCallback fragment, int i, Utils.FileType ft) {
        mTaskId = i;
        mFragment = fragment;
        fileType = ft;
    }

    public void getCertificateFileDialog() {
        Intent startFC = new Intent(getContext(), FileSelect.class);
        startFC.putExtra(FileSelect.START_DATA, mData);
        startFC.putExtra(FileSelect.WINDOW_TITLE, mTitle);
        if (fileType == Utils.FileType.PKCS12)
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
            mDataView.setText(c.getString(R.string.no_data));
            mDataDetails.setText("");
        } else {
            if (mData.startsWith(VpnProfile.DISPLAYNAME_TAG)) {
                mDataView.setText(c.getString(R.string.imported_from_file, VpnProfile.getDisplayName(mData)));
            } else if (mData.startsWith(VpnProfile.INLINE_TAG))
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
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
                Intent startFilePicker = Utils.getFilePickerIntent(fileType);
                mFragment.startActivityForResult(startFilePicker, mTaskId);
            } else {
                getCertificateFileDialog();
            }
        }
    }




    public void setShowClear() {
        mShowClear = true;
    }

}
