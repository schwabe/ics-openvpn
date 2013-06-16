package de.blinkt.openvpn.core;

import android.text.TextUtils;
import de.blinkt.openvpn.VpnProfile;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

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

                return getCertificateFriendlyName(cert);

			} catch (Exception e) {
				OpenVPN.logError("Could not read certificate" + e.getLocalizedMessage());
			}
		}
		return "Could not read/parse certificate";
	}

    public static String getCertificateFriendlyName(X509Certificate cert) {
        X500Principal principal = (X500Principal) cert.getSubjectDN();

        String friendlyName = principal.getName();
        System.out.println(friendlyName);

        // Really evil hack to decode email address
        // See: http://code.google.com/p/android/issues/detail?id=21531

        String[] parts = friendlyName.split(",");
        for (int i=0;i<parts.length;i++){
            String part = parts[i];
            if (part.startsWith("1.2.840.113549.1.9.1=#16")) {
                parts[i] = "email=" + ia5decode(part.replace("1.2.840.113549.1.9.1=#16", ""));
            }
        }
        friendlyName = TextUtils.join(",", parts);
        return friendlyName;
    }

    public static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    private static String ia5decode(String ia5string) {
        String d = "";
        for (int i=1;i<ia5string.length();i=i+2) {
            String hexstr = ia5string.substring(i-1,i+1);
            char c = (char) Integer.parseInt(hexstr,16);
            if (isPrintableChar(c)) {
                d+=c;
            } else if (i==1 && (c==0x12 || c==0x1b)) {
                ;   // ignore
            } else {
                d += "\\x" + hexstr;
            }
        }
        return d;
    }


}
