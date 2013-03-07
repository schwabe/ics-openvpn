package de.blinkt.openvpn;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class RemoteCNPreference extends DialogPreference {


	private Spinner mSpinner;
	private EditText mEditText;
	private int mDNType;
	private ArrayAdapter<String> mAuthtypes;
	private String mDn;

	public RemoteCNPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.tlsremote);

	}

	@Override
	protected void onBindDialogView(View view) {

		super.onBindDialogView(view);

		mEditText = (EditText) view.findViewById(R.id.tlsremotecn);
		mSpinner = (Spinner) view.findViewById(R.id.x509verifytype);
		if(mDn!=null)
			mEditText.setText(mDn);

		populateSpinner();

	}

	private void populateSpinner() {
		mAuthtypes = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item);
		mAuthtypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		mAuthtypes.add(getContext().getString(R.string.complete_dn));
		mAuthtypes.add("RDN (common name)");
		mAuthtypes.add("RDN prefix");
		if (mDNType == VpnProfile.X509_VERIFY_TLSREMOTE || mDNType == VpnProfile.X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING )
			mAuthtypes.add("tls-remote (DEPRECATED)");

		mSpinner.setAdapter(mAuthtypes);
	}

	public String getCNText() {
		return mDn;
	}
	
	public int getAuthtype() {
		return mDNType;
	}

	public void setDN(String dn) {
		mDn = dn;
		if(mEditText!=null)
			mEditText.setText(dn);
	}

	void setAuthType(int x509authtype) {
		mDNType = x509authtype;
		if (mSpinner!=null)
			populateSpinner();
	}

	@Override
	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			String dn = mEditText.getText().toString();
			int authtype = getAuthTypeFromSpinner();
			if (callChangeListener(new Pair<Integer, String>(authtype, dn))) {
				mDn = dn;
				mDNType = authtype;
			}
		}
	}

	private int getAuthTypeFromSpinner() {
		int pos = mSpinner.getSelectedItemPosition();
		switch (pos) {
		case 0:
			return VpnProfile.X509_VERIFY_TLSREMOTE_DN;
		case 1:
			return VpnProfile.X509_VERIFY_TLSREMOTE_RDN;
		case 2:
			return VpnProfile.X509_VERIFY_TLSREMOTE_RDN_PREFIX;
		case 3:
			// This is the tls-remote entry, only visible if mDntype is a
			// tls-remote type
			return mDNType;
		default:
			return VpnProfile.X509_VERIFY_TLSREMOTE;
		}
	}

}
