/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.text.TextUtils;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;


import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;

public class X509Utils {
	public static Certificate getCertificateFromFile(String certfilename) throws FileNotFoundException, CertificateException {
		CertificateFactory certFact = CertificateFactory.getInstance("X.509");

		InputStream inStream;

		if(VpnProfile.isEmbedded(certfilename)) {
            // The java certifcate reader is ... kind of stupid
            // It does NOT ignore chars before the --BEGIN ...
            int subIndex = certfilename.indexOf("-----BEGIN CERTIFICATE-----");
            subIndex = Math.max(0,subIndex);
			inStream = new ByteArrayInputStream(certfilename.substring(subIndex).getBytes());


        } else {
			inStream = new FileInputStream(certfilename);
        }


		return certFact.generateCertificate(inStream);
	}

	public static PemObject readPemObjectFromFile (String keyfilename) throws IOException {

		Reader inStream;

		if(VpnProfile.isEmbedded(keyfilename))
			inStream = new StringReader(VpnProfile.getEmbeddedContent(keyfilename));
		else 
			inStream = new FileReader(new File(keyfilename));

		PemReader pr = new PemReader(inStream);
		PemObject r = pr.readPemObject();
		pr.close();
		return r;
	}




	public static String getCertificateFriendlyName (Context c, String filename) {
		if(!TextUtils.isEmpty(filename)) {
			try {
				X509Certificate cert = (X509Certificate) getCertificateFromFile(filename);

                return getCertificateFriendlyName(cert);

			} catch (Exception e) {
				VpnStatus.logError("Could not read certificate" + e.getLocalizedMessage());
			}
		}
		return c.getString(R.string.cannotparsecert);
	}

    public static String getCertificateFriendlyName(X509Certificate cert) {
        X500Principal principal = cert.getSubjectX500Principal();
        byte[] encodedSubject = principal.getEncoded();
        String friendlyName=null;

        /* Hack so we do not have to ship a whole Spongy/bouncycastle */
        Exception exp=null;
        try {
            Class X509NameClass = Class.forName("com.android.org.bouncycastle.asn1.x509.X509Name");
            Method getInstance = X509NameClass.getMethod("getInstance",Object.class);

            Hashtable defaultSymbols = (Hashtable) X509NameClass.getField("DefaultSymbols").get(X509NameClass);

            if (!defaultSymbols.containsKey("1.2.840.113549.1.9.1"))
                defaultSymbols.put("1.2.840.113549.1.9.1","eMail");

            Object subjectName = getInstance.invoke(X509NameClass, encodedSubject);

            Method toString = X509NameClass.getMethod("toString",boolean.class,Hashtable.class);

            friendlyName= (String) toString.invoke(subjectName,true,defaultSymbols);
                    
        } catch (ClassNotFoundException e) {
            exp =e ;
        } catch (NoSuchMethodException e) {
            exp =e;
        } catch (InvocationTargetException e) {
            exp =e;
        } catch (IllegalAccessException e) {
            exp =e;
        } catch (NoSuchFieldException e) {
            exp =e;
        }
        if (exp!=null)
            VpnStatus.logException("Getting X509 Name from certificate", exp);

        /* Fallback if the reflection method did not work */
        if(friendlyName==null)
            friendlyName = principal.getName();


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
