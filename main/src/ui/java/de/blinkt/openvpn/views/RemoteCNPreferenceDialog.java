/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.views;

import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.preference.PreferenceDialogFragmentCompat;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

import static de.blinkt.openvpn.VpnProfile.X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING;

public class RemoteCNPreferenceDialog extends PreferenceDialogFragmentCompat {
    private Spinner mSpinner;
    private EditText mEditText;
    private TextView mRemoteTLSNote;

    public static RemoteCNPreferenceDialog newInstance(String key) {
        RemoteCNPreferenceDialog f = new RemoteCNPreferenceDialog();
        Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onBindDialogView(View view) {
        String mDn = ((RemoteCNPreference) getPreference()).getCNText();
        int mDNType = ((RemoteCNPreference) getPreference()).getAuthtype();

        mEditText = view.findViewById(R.id.tlsremotecn);
        mSpinner = view.findViewById(R.id.x509verifytype);
        mRemoteTLSNote = view.findViewById(R.id.tlsremotenote);
        mEditText.setText(mDn);

        populateSpinner(mDn, mDNType);
    }

    private void populateSpinner(String mDn, int mDNType) {
        ArrayAdapter<String> authtypes = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item);
        authtypes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        authtypes.add(requireContext().getString(R.string.complete_dn));
        authtypes.add(requireContext().getString(R.string.rdn));
        authtypes.add(requireContext().getString(R.string.rdn_prefix));
        if ((mDNType == VpnProfile.X509_VERIFY_TLSREMOTE || mDNType == X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING)
                && !(mDn == null || "".equals(mDn))) {
            authtypes.add(requireContext().getString(R.string.tls_remote_deprecated));
            mRemoteTLSNote.setVisibility(View.VISIBLE);
        } else {
            mRemoteTLSNote.setVisibility(View.GONE);
        }
        mSpinner.setAdapter(authtypes);
        mSpinner.setSelection(getSpinnerPositionFromAuthTYPE(mDNType, mDn));
    }

    private int getSpinnerPositionFromAuthTYPE(int mDNType, String mDn) {
        switch (mDNType) {
            case VpnProfile.X509_VERIFY_TLSREMOTE_DN:
                return 0;
            case VpnProfile.X509_VERIFY_TLSREMOTE_RDN:
                return 1;
            case VpnProfile.X509_VERIFY_TLSREMOTE_RDN_PREFIX:
                return 2;
            case X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING:
            case VpnProfile.X509_VERIFY_TLSREMOTE:
                if (mDn == null || "".equals(mDn))
                    return 1;
                else
                    return 3;


            default:
                return 0;
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
                return X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING;
            default:
                return VpnProfile.X509_VERIFY_TLSREMOTE;
        }
    }


    @Override
    public void onDialogClosed(boolean positiveResult) {

        if (positiveResult) {
            RemoteCNPreference pref = ((RemoteCNPreference) getPreference());

            String dn = mEditText.getText().toString();
            int authtype = getAuthTypeFromSpinner();
            if (pref.callChangeListener(new Pair<>(authtype, dn))) {
                pref.setDN(dn);
                pref.setAuthType(authtype);
            }
        }
    }


}
