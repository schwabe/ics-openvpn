package de.blinkt.openvpn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.util.Base64;
import de.blinkt.openvpn.core.NativeUtils;
import de.blinkt.openvpn.core.OpenVPN;
import de.blinkt.openvpn.core.OpenVpnService;
import de.blinkt.openvpn.core.X509Utils;
import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemWriter;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import java.util.Vector;

public class VpnProfile implements  Serializable{
	// Note that this class cannot be moved to core where it belongs since 
	// the profile loading depends on it being here
	// The Serializable documentation mentions that class name change are possible
	// but the how is unclear
	// 

	private static final long serialVersionUID = 7085688938959334563L;
	public static final int TYPE_CERTIFICATES=0;
	public static final int TYPE_PKCS12=1;
	public static final int TYPE_KEYSTORE=2;
	public static final int TYPE_USERPASS = 3;
	public static final int TYPE_STATICKEYS = 4;
	public static final int TYPE_USERPASS_CERTIFICATES = 5;
	public static final int TYPE_USERPASS_PKCS12 = 6;
	public static final int TYPE_USERPASS_KEYSTORE = 7;

	public static final int X509_VERIFY_TLSREMOTE=0;
	public static final int X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING=1;
	public static final int X509_VERIFY_TLSREMOTE_DN=2;
	public static final int X509_VERIFY_TLSREMOTE_RDN=3;
	public static final int X509_VERIFY_TLSREMOTE_RDN_PREFIX=4;



	// Don't change this, not all parts of the program use this constant
	public static final String EXTRA_PROFILEUUID = "de.blinkt.openvpn.profileUUID";
	public static final String INLINE_TAG = "[[INLINE]]";
	private static final String OVPNCONFIGFILE = "android.conf";

	public transient String mTransientPW=null;
	public transient String mTransientPCKS12PW=null;
	private transient PrivateKey mPrivateKey;

	// variable named wrong and should haven beeen transient
	// but needs to keep wrong name to guarante loading of old
	// profiles
	public transient boolean profileDleted=false;


	public static String DEFAULT_DNS1="131.234.137.23";
	public static String DEFAULT_DNS2="131.234.137.24";

	// Public attributes, since I got mad with getter/setter
	// set members to default values
	private UUID mUuid;
	public int mAuthenticationType = TYPE_KEYSTORE ;
	public String mName;
	public String mAlias;
	public String mClientCertFilename;
	public String mTLSAuthDirection="";
	public String mTLSAuthFilename;
	public String mClientKeyFilename;
	public String mCaFilename;
	public boolean mUseLzo=true;
	public String mServerPort= "1194" ;
	public boolean mUseUdp = true;
	public String mPKCS12Filename;
	public String mPKCS12Password;
	public boolean mUseTLSAuth = false;
	public String mServerName = "openvpn.blinkt.de" ;
	public String mDNS1=DEFAULT_DNS1;
	public String mDNS2=DEFAULT_DNS2;
	public String mIPv4Address;
	public String mIPv6Address;
	public boolean mOverrideDNS=false;
	public String mSearchDomain="blinkt.de";
	public boolean mUseDefaultRoute=true;
	public boolean mUsePull=true;
	public String mCustomRoutes;
	public boolean mCheckRemoteCN=false;
	public boolean mExpectTLSCert=true;
	public String mRemoteCN="";
	public String mPassword="";
	public String mUsername="";
	public boolean mRoutenopull=false;
	public boolean mUseRandomHostname=false;
	public boolean mUseFloat=false;
	public boolean mUseCustomConfig=false;
	public String mCustomConfigOptions="";
	public String mVerb="1";
	public String mCipher="";
	public boolean mNobind=false;
	public boolean mUseDefaultRoutev6=true;
	public String mCustomRoutesv6="";
	public String mKeyPassword="";
	public boolean mPersistTun = false;
	public String mConnectRetryMax="5";
	public String mConnectRetry="5";
	public boolean mUserEditable=true;
	public String mAuth="";
	public int mX509AuthType=X509_VERIFY_TLSREMOTE_RDN;

	public static final String MINIVPN = "miniopenvpn";

	public void clearDefaults() {
		mServerName="unkown";
		mUsePull=false;
		mUseLzo=false;
		mUseDefaultRoute=false;
		mUseDefaultRoutev6=false;
		mExpectTLSCert=false;
		mPersistTun = false;
	}


	public static String openVpnEscape(String unescaped) {
		if(unescaped==null)
			return null;
		String escapedString = unescaped.replace("\\", "\\\\");
		escapedString = escapedString.replace("\"","\\\"");
		escapedString = escapedString.replace("\n","\\n");

		if (escapedString.equals(unescaped) && !escapedString.contains(" ") && !escapedString.contains("#"))
			return unescaped;
		else
			return '"' + escapedString + '"';
	}

	public VpnProfile(String name) {
		mUuid = UUID.randomUUID();
		mName = name;
	}

	public UUID getUUID() {
		return mUuid;

	}

	public String getName() {
		return mName;
	}


	public String getConfigFile(Context context, boolean configForOvpn3)
	{

		File cacheDir= context.getCacheDir();
		String cfg="";

		// Enable managment interface
		cfg += "# Enables connection to GUI\n";
		cfg += "management ";

		cfg +=cacheDir.getAbsolutePath() + "/" +  "mgmtsocket";
		cfg += " unix\n";
		cfg += "management-client\n";
		// Not needed, see updated man page in 2.3
		//cfg += "management-signal\n";
		cfg += "management-query-passwords\n";
		cfg += "management-hold\n\n";
        cfg += getVersionEnvString(context);

		cfg+="# Log window is better readable this way\n";
		cfg+="suppress-timestamps\n";



		boolean useTLSClient = (mAuthenticationType != TYPE_STATICKEYS);

		if(useTLSClient && mUsePull)
			cfg+="client\n";
		else if (mUsePull)
			cfg+="pull\n";
		else if(useTLSClient)
			cfg+="tls-client\n";


		cfg+="verb " + mVerb + "\n";

		if(mConnectRetryMax ==null) {
			mConnectRetryMax="5";
		}

		if(!mConnectRetryMax.equals("-1"))
			cfg+="connect-retry-max " + mConnectRetryMax+ "\n";

		if(mConnectRetry==null)
			mConnectRetry="5";


		cfg+="connect-retry " + mConnectRetry + "\n";

		cfg+="resolv-retry 60\n";



		// We cannot use anything else than tun
		cfg+="dev tun\n";

		// Server Address
		cfg+="remote ";
		cfg+=mServerName;
		cfg+=" ";
		cfg+=mServerPort;
		if(mUseUdp)
			cfg+=" udp\n";
		else
			cfg+=" tcp-client\n";




		switch(mAuthenticationType) {
		case VpnProfile.TYPE_USERPASS_CERTIFICATES:
			cfg+="auth-user-pass\n";
		case VpnProfile.TYPE_CERTIFICATES:
			// Ca
			cfg+=insertFileData("ca",mCaFilename);

			// Client Cert + Key
			cfg+=insertFileData("key",mClientKeyFilename);
			cfg+=insertFileData("cert",mClientCertFilename);

			break;
		case VpnProfile.TYPE_USERPASS_PKCS12:
			cfg+="auth-user-pass\n";
		case VpnProfile.TYPE_PKCS12:
			cfg+=insertFileData("pkcs12",mPKCS12Filename);
			break;

		case VpnProfile.TYPE_USERPASS_KEYSTORE:
			cfg+="auth-user-pass\n";
		case VpnProfile.TYPE_KEYSTORE:
			if(!configForOvpn3) {
				String[] ks =getKeyStoreCertificates(context);
				cfg+="### From Keystore ####\n";
				if(ks != null) {
					cfg+="<ca>\n" + ks[0] + "</ca>\n";
					cfg+="<cert>\n" + ks[0] + "</cert>\n";
					cfg+="management-external-key\n";
				} else {
					cfg += context.getString(R.string.keychain_access) +"\n";
					if(Build.VERSION.SDK_INT==Build.VERSION_CODES.JELLY_BEAN) 
						if(!mAlias.matches("^[a-zA-Z0-9]$")) 
							cfg += context.getString(R.string.jelly_keystore_alphanumeric_bug)+ "\n";
				}
			}
			break;
		case VpnProfile.TYPE_USERPASS:
			cfg+="auth-user-pass\n";
			cfg+=insertFileData("ca",mCaFilename);
		}

		if(mUseLzo) {
			cfg+="comp-lzo\n";
		}

		if(mUseTLSAuth) {
			if(mAuthenticationType==TYPE_STATICKEYS)
				cfg+=insertFileData("secret",mTLSAuthFilename);
			else
				cfg+=insertFileData("tls-auth",mTLSAuthFilename);

			if(nonNull(mTLSAuthDirection)) {
				cfg+= "key-direction ";
				cfg+= mTLSAuthDirection;
				cfg+="\n";
			}

		}

		if(!mUsePull ) {
			if(nonNull(mIPv4Address))
				cfg +="ifconfig " + cidrToIPAndNetmask(mIPv4Address) + "\n";

			if(nonNull(mIPv6Address))
				cfg +="ifconfig-ipv6 " + mIPv6Address + "\n";
		}

		if(mUsePull && mRoutenopull)
			cfg += "route-nopull\n";

		String routes = "";
		int numroutes=0;
		if(mUseDefaultRoute)
			routes += "route 0.0.0.0 0.0.0.0\n";
		else
			for(String route:getCustomRoutes()) {
				routes += "route " + route + "\n";
				numroutes++;
			}


		if(mUseDefaultRoutev6)
			cfg += "route-ipv6 ::/0\n";
		else
			for(String route:getCustomRoutesv6()) {
				routes += "route-ipv6 " + route + "\n";
				numroutes++;
			}

		// Round number to next 100 
		if(numroutes> 90) {
			numroutes = ((numroutes / 100)+1) * 100;
			cfg+="# Alot of routes are set, increase max-routes\n";
			cfg+="max-routes " + numroutes + "\n";
		}
		cfg+=routes;

		if(mOverrideDNS || !mUsePull) {
			if(nonNull(mDNS1))
				cfg+="dhcp-option DNS " + mDNS1 + "\n";
			if(nonNull(mDNS2))
				cfg+="dhcp-option DNS " + mDNS2 + "\n";
			if(nonNull(mSearchDomain))
				cfg+="dhcp-option DOMAIN " + mSearchDomain + "\n";

		}

		if(mNobind)
			cfg+="nobind\n";



		// Authentication
		if(mCheckRemoteCN) {
			if(mRemoteCN == null || mRemoteCN.equals("") )
				cfg+="verify-x509-name " + mServerName + " name\n";
			else 
				switch (mX509AuthType) {

				// 2.2 style x509 checks
				case X509_VERIFY_TLSREMOTE_COMPAT_NOREMAPPING:
					cfg+="compat-names no-remapping\n";
				case X509_VERIFY_TLSREMOTE:
					cfg+="tls-remote " + openVpnEscape(mRemoteCN) + "\n";
					break;

				case X509_VERIFY_TLSREMOTE_RDN:
					cfg+="verify-x509-name " + openVpnEscape(mRemoteCN) + " name\n";
					break;

				case X509_VERIFY_TLSREMOTE_RDN_PREFIX:
					cfg+="verify-x509-name " + openVpnEscape(mRemoteCN) + " name-prefix\n";
					break;

				case X509_VERIFY_TLSREMOTE_DN:
					cfg+="verify-x509-name " + openVpnEscape(mRemoteCN) + "\n";
					break;
				}
		}
		if(mExpectTLSCert)
			cfg += "remote-cert-tls server\n";


		if(nonNull(mCipher)){
			cfg += "cipher " + mCipher + "\n";
		}

		if(nonNull(mAuth)) {
			cfg += "auth " + mAuth + "\n";
		}

		// Obscure Settings dialog
		if(mUseRandomHostname)
			cfg += "#my favorite options :)\nremote-random-hostname\n";

		if(mUseFloat)
			cfg+= "float\n";

		if(mPersistTun) {
			cfg+= "persist-tun\n";
			cfg+= "# persist-tun also sets persist-remote-ip to avoid DNS resolve problem\n";
			cfg+= "persist-remote-ip\n";
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);        
		boolean usesystemproxy = prefs.getBoolean("usesystemproxy", true);
		if(usesystemproxy) {
			cfg+= "# Use system proxy setting\n";
			cfg+= "management-query-proxy\n";
		}


		if(mUseCustomConfig) {
			cfg += "# Custom configuration options\n";
			cfg += "# You are on your on own here :)\n";
			cfg += mCustomConfigOptions;
			cfg += "\n";

		}



		return cfg;
	}

    private String getVersionEnvString(Context c) {
        String version="unknown";
        try {
            PackageInfo packageinfo = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            version = packageinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return  String.format(Locale.US,"setenv IV_OPENVPN_GUI_VERSION \"%s %s\"\n",c.getPackageName(),version);

    }

    //! Put inline data inline and other data as normal escaped filename
	private String insertFileData(String cfgentry, String filedata) {
		if(filedata==null) {
			// TODO: generate good error
			return String.format("%s %s\n",cfgentry,"missing");
		}else if(filedata.startsWith(VpnProfile.INLINE_TAG)){
			String datawoheader = filedata.substring(VpnProfile.INLINE_TAG.length());
			return String.format(Locale.ENGLISH,"<%s>\n%s\n</%s>\n",cfgentry,datawoheader,cfgentry);
		} else {
			return String.format(Locale.ENGLISH,"%s %s\n",cfgentry,openVpnEscape(filedata));
		}
	}

	private boolean nonNull(String val) {
		if(val == null || val.equals("")) 
			return false;
		else
			return true;
	}

	private Collection<String> getCustomRoutes() {
		Vector<String> cidrRoutes=new Vector<String>();
		if(mCustomRoutes==null) {
			// No routes set, return empty vector
			return cidrRoutes;
		}
		for(String route:mCustomRoutes.split("[\n \t]")) {
			if(!route.equals("")) {
				String cidrroute = cidrToIPAndNetmask(route);
				if(cidrRoutes == null)
					return null;

				cidrRoutes.add(cidrroute);
			}
		}

		return cidrRoutes;
	}

	private Collection<String> getCustomRoutesv6() {
		Vector<String> cidrRoutes=new Vector<String>();
		if(mCustomRoutesv6==null) {
			// No routes set, return empty vector
			return cidrRoutes;
		}
		for(String route:mCustomRoutesv6.split("[\n \t]")) {
			if(!route.equals("")) {
				cidrRoutes.add(route);
			}
		}

		return cidrRoutes;
	}



	private String cidrToIPAndNetmask(String route) {
		String[] parts = route.split("/");

		// No /xx, assume /32 as netmask
		if (parts.length ==1)
			parts = (route + "/32").split("/");

		if (parts.length!=2)
			return null;
		int len;
		try { 
			len = Integer.parseInt(parts[1]);
		}	catch(NumberFormatException ne) {
			return null;
		}
		if (len <0 || len >32)
			return null;


		long nm = 0xffffffffl;
		nm = (nm << (32-len)) & 0xffffffffl;

		String netmask =String.format(Locale.ENGLISH,"%d.%d.%d.%d", (nm & 0xff000000) >> 24,(nm & 0xff0000) >> 16, (nm & 0xff00) >> 8 ,nm & 0xff  );	
		return parts[0] + "  " + netmask;
	}



	private String[] buildOpenvpnArgv(File cacheDir)
	{
		Vector<String> args = new Vector<String>();

		// Add fixed paramenters
		//args.add("/data/data/de.blinkt.openvpn/lib/openvpn");
		args.add(cacheDir.getAbsolutePath() +"/" + VpnProfile.MINIVPN);

		args.add("--config");
		args.add(cacheDir.getAbsolutePath() + "/" + OVPNCONFIGFILE);


		return  (String[]) args.toArray(new String[args.size()]);
	}

	public Intent prepareIntent(Context context) {
		String prefix = context.getPackageName();

		Intent intent = new Intent(context,OpenVpnService.class);

		if(mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
			if(getKeyStoreCertificates(context)==null)
				return null;
		}

		intent.putExtra(prefix + ".ARGV" , buildOpenvpnArgv(context.getCacheDir()));
		intent.putExtra(prefix + ".profileUUID", mUuid.toString());

		ApplicationInfo info = context.getApplicationInfo();
		intent.putExtra(prefix +".nativelib",info.nativeLibraryDir);

		try {
			FileWriter cfg = new FileWriter(context.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE);
			cfg.write(getConfigFile(context,false));
			cfg.flush();
			cfg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return intent;
	}

	String[] getKeyStoreCertificates(Context context) {
		PrivateKey privateKey = null;
		X509Certificate[] cachain=null;
		try {
			privateKey = KeyChain.getPrivateKey(context,mAlias);
			mPrivateKey = privateKey;

			cachain = KeyChain.getCertificateChain(context, mAlias);
			if(cachain.length <= 1 && !nonNull(mCaFilename))
				OpenVPN.logMessage(0, "", context.getString(R.string.keychain_nocacert));

			for(X509Certificate cert:cachain) {
				OpenVPN.logInfo(R.string.cert_from_keystore,X509Utils.getCertificateFriendlyName(cert));
			}




			if(nonNull(mCaFilename)) {
				try {
					Certificate cacert = X509Utils.getCertificateFromFile(mCaFilename);
					X509Certificate[] newcachain = new X509Certificate[cachain.length+1];
					for(int i=0;i<cachain.length;i++)
						newcachain[i]=cachain[i];

					newcachain[cachain.length-1]=(X509Certificate) cacert;

				} catch (Exception e) {
					OpenVPN.logError("Could not read CA certificate" + e.getLocalizedMessage());
				}
			}



			StringWriter caout = new StringWriter();

			PemWriter pw = new PemWriter(caout);
			for(X509Certificate cert:cachain) {
				pw.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
			}
			pw.close();



			StringWriter certout = new StringWriter();


			if(cachain.length>= 1){
				X509Certificate usercert = cachain[0];

				PemWriter upw = new PemWriter(certout);
				upw.writeObject(new PemObject("CERTIFICATE", usercert.getEncoded()));
				upw.close();

			}

			return new String[] {caout.toString(),certout.toString()};
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyChainException e) {
			OpenVPN.logMessage(0,"",context.getString(R.string.keychain_access));
			if(Build.VERSION.SDK_INT==Build.VERSION_CODES.JELLY_BEAN){
				if(!mAlias.matches("^[a-zA-Z0-9]$")) {
					OpenVPN.logError(R.string.jelly_keystore_alphanumeric_bug);
				}
			}
		}
		return null;
	}


	//! Return an error if somethign is wrong
	public int checkProfile(Context context) {
		if(mAuthenticationType==TYPE_KEYSTORE || mAuthenticationType==TYPE_USERPASS_KEYSTORE) {
			if(mAlias==null) 
				return R.string.no_keystore_cert_selected;
		}

		if(!mUsePull) {
			if(mIPv4Address == null || cidrToIPAndNetmask(mIPv4Address) == null)
				return R.string.ipv4_format_error;
		}
		if(!mUseDefaultRoute && getCustomRoutes()==null)
			return R.string.custom_route_format_error;

		// Everything okay
		return R.string.no_error_found;

	}



	//! Openvpn asks for a "Private Key", this should be pkcs12 key
	//
	public String getPasswordPrivateKey() {
		if(mTransientPCKS12PW!=null) {
			String pwcopy = mTransientPCKS12PW;
			mTransientPCKS12PW=null;
			return pwcopy;
		}
		switch (mAuthenticationType) {
		case TYPE_PKCS12:
		case TYPE_USERPASS_PKCS12:
			return mPKCS12Password;

		case TYPE_CERTIFICATES:
		case TYPE_USERPASS_CERTIFICATES:
			return mKeyPassword;

		case TYPE_USERPASS:
		case TYPE_STATICKEYS:
		default:
			return null;
		}
	}
	boolean isUserPWAuth() {
		switch(mAuthenticationType) {
		case TYPE_USERPASS:
		case TYPE_USERPASS_CERTIFICATES:
		case TYPE_USERPASS_KEYSTORE:
		case TYPE_USERPASS_PKCS12:
			return true;
		default:
			return false;

		}
	}


	public boolean requireTLSKeyPassword() {
		if(!nonNull(mClientKeyFilename))
			return false;

		String data = "";
		if(mClientKeyFilename.startsWith(INLINE_TAG))
			data = mClientKeyFilename;
		else {
			char[] buf = new char[2048];
			FileReader fr;
			try {
				fr = new FileReader(mClientKeyFilename);
				int len = fr.read(buf);
				while(len > 0 ) {
					data += new String(buf,0,len);
					len = fr.read(buf);
				}
				fr.close();
			} catch (FileNotFoundException e) {
				return false;
			} catch (IOException e) {
				return false;
			}

		}

		if(data.contains("Proc-Type: 4,ENCRYPTED"))
			return true;
		else if(data.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----"))
			return true;
		else
			return false;
	}

	public int needUserPWInput() {
		if((mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12)&&
				(mPKCS12Password == null || mPKCS12Password.equals(""))) {
			if(mTransientPCKS12PW==null)
				return R.string.pkcs12_file_encryption_key;
		}

		if(mAuthenticationType == TYPE_CERTIFICATES || mAuthenticationType == TYPE_USERPASS_CERTIFICATES) {
			if(requireTLSKeyPassword() && !nonNull(mKeyPassword))
				if(mTransientPCKS12PW==null) {
					return R.string.private_key_password;
				}
		}

		if (isUserPWAuth() && !(nonNull(mUsername) && (nonNull(mPassword) || mTransientPW!=null))) {
				return R.string.password;
		}
		return 0;
	}

	public String getPasswordAuth() {
		if(mTransientPW!=null) {
			String pwcopy = mTransientPW;
			mTransientPW=null;
			return pwcopy;
		} else {
			return mPassword;
		}
	}


	// Used by the Array Adapter
	@Override
	public String toString() {
		return mName;
	}


	public String getUUIDString() {
		return mUuid.toString();
	}


	public PrivateKey getKeystoreKey() {
		return mPrivateKey;
	}

	public String getSignedData(String b64data) {
		PrivateKey privkey = getKeystoreKey();
		Exception err =null;

		byte[] data = Base64.decode(b64data, Base64.DEFAULT);

		// The Jelly Bean *evil* Hack
		// 4.2 implements the RSA/ECB/PKCS1PADDING in the OpenSSLprovider
		if(Build.VERSION.SDK_INT==Build.VERSION_CODES.JELLY_BEAN){
			return processSignJellyBeans(privkey,data);
		}


		try{


			Cipher rsasinger = Cipher.getInstance("RSA/ECB/PKCS1PADDING");

			rsasinger.init(Cipher.ENCRYPT_MODE, privkey);

			byte[] signed_bytes = rsasinger.doFinal(data);
			return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

		} catch (NoSuchAlgorithmException e){
			err =e;
		} catch (InvalidKeyException e) {
			err =e;
		} catch (NoSuchPaddingException e) {
			err =e;
		} catch (IllegalBlockSizeException e) {
			err =e;
		} catch (BadPaddingException e) {
			err =e;
		}
		if(err !=null) {
			OpenVPN.logError(R.string.error_rsa_sign,err.getClass().toString(),err.getLocalizedMessage());
		}
		return null;

	}


	private String processSignJellyBeans(PrivateKey privkey, byte[] data) {
		Exception err =null;
		try {
			Method[] allm = privkey.getClass().getSuperclass().getDeclaredMethods();
			System.out.println(allm);
			Method getKey = privkey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey");
			getKey.setAccessible(true);

			// Real object type is OpenSSLKey
			Object opensslkey = getKey.invoke(privkey);

			getKey.setAccessible(false);

			Method getPkeyContext = opensslkey.getClass().getDeclaredMethod("getPkeyContext");

			// integer pointer to EVP_pkey
			getPkeyContext.setAccessible(true);
			int pkey = (Integer) getPkeyContext.invoke(opensslkey);
			getPkeyContext.setAccessible(false);

			byte[] signed_bytes = NativeUtils.rsasign(data, pkey); 
			return Base64.encodeToString(signed_bytes, Base64.NO_WRAP);

		} catch (NoSuchMethodException e) {
			err=e;
		} catch (IllegalArgumentException e) {
			err=e;
		} catch (IllegalAccessException e) {
			err=e;
		} catch (InvocationTargetException e) {
			err=e;
		} catch (InvalidKeyException e) {
			err=e;
		}
		if(err !=null) {
			OpenVPN.logError(R.string.error_rsa_sign,err.getClass().toString(),err.getLocalizedMessage());
		}
		return null;

	}



}




