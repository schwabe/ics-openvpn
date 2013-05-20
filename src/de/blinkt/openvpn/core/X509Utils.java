package de.blinkt.openvpn.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;

import android.text.TextUtils;

import de.blinkt.openvpn.VpnProfile;

public class X509Utils {
	public static Certificate getCertificateFromFile(String certfilename) throws FileNotFoundException, CertificateException {
		CertificateFactory certFact = CertificateFactory.getInstance("X.509");

		InputStream inStream;

		if(certfilename.startsWith(VpnProfile.INLINE_TAG))
			inStream = new ByteArrayInputStream(certfilename.replace(VpnProfile.INLINE_TAG,"").getBytes());
		else 
			inStream = new FileInputStream(certfilename);

		return certFact.generateCertificate(inStream);
	}

	public static PemObject readPemObjectFromFile (String keyfilename) throws CertificateException, IOException {

		Reader inStream;

		if(keyfilename.startsWith(VpnProfile.INLINE_TAG))
			inStream = new StringReader(keyfilename.replace(VpnProfile.INLINE_TAG,""));
		else 
			inStream = new FileReader(new File(keyfilename));

		PemReader pr = new PemReader(inStream);
		PemObject r = pr.readPemObject();
		pr.close();
		return r;
	}




	public static String getCertificateFriendlyName (String filename) {
		if(!TextUtils.isEmpty(filename)) {
			try {
				X509Certificate cert = (X509Certificate) getCertificateFromFile(filename);
				
				String friendly = cert.getSubjectDN().getName();
							
				return friendly;

			} catch (Exception e) {
				OpenVPN.logError("Could not read certificate" + e.getLocalizedMessage());
			}
		}
		return "Could not read/parse certificate";
	}


}
