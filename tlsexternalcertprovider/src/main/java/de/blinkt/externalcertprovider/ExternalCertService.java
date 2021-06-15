/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.externalcertprovider;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import de.blinkt.openvpn.api.ExternalCertificateProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static de.blinkt.externalcertprovider.SelectCertificateActivity.EXTRA_ALIAS;
import static de.blinkt.externalcertprovider.SelectCertificateActivity.EXTRA_DESCRIPTION;

/**
 * This is a VERY basic implementation.
 * It does not even check if the service is even allowed to use the API
 * see ExternalOpenVPNService for an example of checking caller's creditionals
 */
public class ExternalCertService extends Service {
    private byte[] doSign(byte[] data)
    {
        try {
            return SimpleSigner.signData(data, false);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        // Something failed, return null
        return null;
    }

    private final ExternalCertificateProvider.Stub mBinder = new ExternalCertificateProvider.Stub() {



        @Override
        public byte[] getSignedData(String alias, byte[] data) throws RemoteException {

            return null;

        }

        @Override
        public byte[] getCertificateChain(String alias) throws RemoteException {

            return TextUtils.join("\n", SimpleSigner.certchain).getBytes();
        }

        @Override
        public Bundle getCertificateMetaData(String alias){
            Bundle b = new Bundle();
            b.putString(EXTRA_ALIAS, "mynicecert");
            b.putString(EXTRA_DESCRIPTION, "Super secret example key!");
            return b;
        }

        @Override
        public byte[] getSignedDataWithExtra(String alias, byte[] data, Bundle extra) throws RemoteException {
            return new byte[0];
        }
    };



    @Override
    public void onCreate() {
        super.onCreate();

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}