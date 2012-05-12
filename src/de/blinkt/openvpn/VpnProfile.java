package de.blinkt.openvpn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.security.KeyChain;
import android.security.KeyChainException;

public class VpnProfile implements  Serializable{
	// Parcable
	/**
	 * 
	 */
	private static final long serialVersionUID = 7085688938959334563L;
	static final int TYPE_CERTIFICATES=0;
	static final int TYPE_PKCS12=1;
	static final int TYPE_KEYSTORE=2;
	public static final int TYPE_USERPASS = 3;
	public static final int TYPE_STATICKEYS = 4;
	public static final int TYPE_USERPASS_CERTIFICATES = 5;
	public static final int TYPE_USERPASS_PKCS12 = 6;
	public static final int TYPE_USERPASS_KEYSTORE = 7;
	
	// Don't change this, not all parts of the program use this constant
	public static final String EXTRA_PROFILEUUID = "de.blinkt.openvpn.profileUUID";
	public static final String INLINE_TAG = "[[INLINE]]";
	private static final String OVPNCONFIGFILE = "android.conf";

	protected transient String mTransientPW=null;
	protected transient String mTransientPCKS12PW=null;

	private static transient String mTempPKCS12Password;

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


	public void clearDefaults() {
		mServerName="unkown";
		mUsePull=false;
		mUseLzo=false;
		mUseDefaultRoute=false;
		mExpectTLSCert=false;
	}


	public static String openVpnEscape(String unescaped) {
		if(unescaped==null)
			return null;
		String escapedString = unescaped.replace("\\", "\\\\");
		escapedString = escapedString.replace("\"","\\\"");
		escapedString = escapedString.replace("\n","\\n");
		return '"' + escapedString + '"';
	}
	

	static final String OVPNCONFIGPKCS12 = "android.pkcs12";


	public VpnProfile(String name) {
		mUuid = UUID.randomUUID();
		mName = name;
	}

	public UUID getUUID() {
		return mUuid;

	}

	public String getName() {
		// TODO Auto-generated method stub
		return mName;
	}


	public String getConfigFile(File cacheDir)
	{

		String cfg="";

		// Enable managment interface
		cfg += "# Enables connection to GUI\n";
		cfg += "management ";

		cfg +=cacheDir.getAbsolutePath() + "/" +  "mgmtsocket";
		cfg += " unix\n";
		cfg += "management-hold\n\n";

		cfg+="# /tmp does not exist on Android\n";
		cfg+="tmp-dir ";
		cfg+=cacheDir.getAbsolutePath();
		cfg+="\n\n";
		
		

		boolean useTLSClient = (mAuthenticationType != TYPE_STATICKEYS);

		if(useTLSClient && mUsePull)
			cfg+="client\n";
		else if (mUsePull)
			cfg+="pull\n";
		else if(useTLSClient)
			cfg+="tls-client\n";


		cfg+="verb " + mVerb + "\n";




		// quit after 5 tries
		cfg+="connect-retry-max 5\n";
		cfg+="resolv-retry 5\n";



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
			cfg+=" tcp\n";




		switch(mAuthenticationType) {
		case VpnProfile.TYPE_USERPASS_CERTIFICATES:
			cfg+="auth-user-pass\n";
			cfg+="management-query-passwords\n";
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
			cfg+="management-query-passwords\n";
			break;

		case VpnProfile.TYPE_USERPASS_KEYSTORE:
			cfg+="auth-user-pass\n";
		case VpnProfile.TYPE_KEYSTORE:
			cfg+="pkcs12 ";
			cfg+=cacheDir.getAbsolutePath() + "/" + OVPNCONFIGPKCS12;
			cfg+="\n";
			cfg+="management-query-passwords\n";
			break;
		case VpnProfile.TYPE_USERPASS:
			cfg+="auth-user-pass\n";
			cfg+="management-query-passwords\n";
			cfg+=insertFileData("ca",mCaFilename);
		}

		if(mUseLzo) {
			cfg+="comp-lzo\n";
		}

		if(mUseTLSAuth) {
			if(mAuthenticationType==TYPE_STATICKEYS)
				cfg+=insertFileData("scecret",mTLSAuthFilename);
			else
				cfg+=insertFileData("tls-auth",mTLSAuthFilename);
			cfg+=" ";
		
			if(nonNull(mTLSAuthDirection)) {
				cfg+= "key-direction ";
				cfg+= mTLSAuthDirection;
			}
			cfg+="\n";
		}

		// Basic Settings
		if(!mUsePull ) {
			cfg +="ifconfig " + cidrToIPAndNetmask(mIPv4Address) + "\n";
		}

		if(mUsePull && mRoutenopull)
			cfg += "route-nopull\n";

		if(mUseDefaultRoute)
			cfg += "route 0.0.0.0 0.0.0.0\n";
		else
			for(String route:getCustomRoutes()) {
				cfg += "route " + route + "\n";
			}


		if(mOverrideDNS || !mUsePull) {
			if(!mDNS1.equals("") && mDNS1!=null)
				cfg+="dhcp-option DNS " + mDNS1 + "\n";
			if(!mDNS2.equals("") && mDNS2!=null)
				cfg+="dhcp-option DNS " + mDNS2 + "\n";

		}
		
		if(mNobind)
			cfg+="nobind\n";



		// Authentication
		if(mCheckRemoteCN) {
			if(mRemoteCN == null || mRemoteCN.equals("") )
				cfg+="tls-remote " + mServerName + "\n";
			else
				cfg += "tls-remote " + mRemoteCN + "\n";
		}
		if(mExpectTLSCert)
			cfg += "remote-cert-tls server\n";


		if(nonNull(mCipher)){
			cfg += "cipher " + mCipher + "\n";
		}


		// Obscure Settings dialog
		if(mUseRandomHostname)
			cfg += "#my favorite options :)\nremote-random-hostname\n";

		if(mUseFloat)
			cfg+= "float\n";

		if(mUseCustomConfig) {
			cfg += "# Custom configuration options\n";
			cfg += "# You are on your on own here :)\n";
			cfg += mCustomConfigOptions;
			cfg += "\n";

		}



		return cfg;
	}

	//! Put inline data inline and other data as normal escaped filename
	private String insertFileData(String cfgentry, String filedata) {
		if(filedata==null) {
			return String.format("%s %s\n",cfgentry,"missing");
		}else if(filedata.startsWith(VpnProfile.INLINE_TAG)){
			String datawoheader = filedata.substring(VpnProfile.INLINE_TAG.length());
			return String.format("<%s>\n%s\n</%s>\n",cfgentry,datawoheader,cfgentry);
		} else {
			return String.format("%s %s\n",cfgentry,openVpnEscape(filedata));
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

	private String cidrToIPAndNetmask(String route) {
		String[] parts = route.split("/");

		// No /xx, return verbatim
		if (parts.length ==1)
			return route;

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

		String netmask =String.format("%d.%d.%d.%d", (nm & 0xff000000) >> 24,(nm & 0xff0000) >> 16, (nm & 0xff00) >> 8 ,nm & 0xff  );	
		return parts[0] + "  " + netmask;
	}



	private String[] buildOpenvpnArgv(File cacheDir)
	{
		Vector<String> args = new Vector<String>();

		// Add fixed paramenters
		//args.add("/data/data/de.blinkt.openvpn/lib/openvpn");
		args.add(cacheDir.getAbsolutePath() +"/" +"minivpn");

		args.add("--config");
		args.add(cacheDir.getAbsolutePath() + "/" + OVPNCONFIGFILE);
		// Silences script security warning
		
		args.add("script-security");
		args.add("0");
		

		return  (String[]) args.toArray(new String[args.size()]);
	}

	public Intent prepareIntent(Context context) {
		String prefix = context.getPackageName();

		Intent intent = new Intent(context,OpenVpnService.class);

		if(mAuthenticationType == VpnProfile.TYPE_KEYSTORE || mAuthenticationType == VpnProfile.TYPE_USERPASS_KEYSTORE) {
			savePKCS12(context);
		}

		intent.putExtra(prefix + ".ARGV" , buildOpenvpnArgv(context.getCacheDir()));
		intent.putExtra(prefix + ".profileUUID", mUuid.toString());
		
		ApplicationInfo info = context.getApplicationInfo();
		intent.putExtra(prefix +".nativelib",info.nativeLibraryDir);

		try {
			FileWriter cfg = new FileWriter(context.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE);
			cfg.write(getConfigFile(context.getCacheDir()));
			cfg.flush();
			cfg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return intent;
	}

	private String getTemporaryPKCS12Password() {
		if(mTempPKCS12Password!=null)
			return mTempPKCS12Password;

		String pw= "";
		// Put enough digits togher to make a password :)
		Random r = new Random();
		for(int i=0;i < 4;i++) {
			pw += new Integer(r.nextInt(1000)).toString();
		}

		mTempPKCS12Password=pw;
		return mTempPKCS12Password;

	}

	private void savePKCS12(Context context) {
		PrivateKey privateKey = null;
		X509Certificate[] cachain=null;
		try {
			privateKey = KeyChain.getPrivateKey(context,mAlias);
			cachain = KeyChain.getCertificateChain(context, mAlias);

			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(null, null);
			ks.setKeyEntry("usercert", privateKey, null, cachain);
			String mypw = getTemporaryPKCS12Password();
			FileOutputStream fout = new FileOutputStream(context.getCacheDir().getAbsolutePath() + "/" + VpnProfile.OVPNCONFIGPKCS12);
			ks.store(fout,mypw.toCharArray());
			fout.flush(); fout.close();
			return;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyChainException e) {
			OpenVPN.logMessage(0,"",context.getString(R.string.keychain_access));
		}

	}
	//! Return an error if somethign is wrong
	int checkProfile() {
		if((mAuthenticationType==TYPE_KEYSTORE || mAuthenticationType==TYPE_USERPASS_KEYSTORE) && mAlias==null) 
			return R.string.no_keystore_cert_selected;

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
		case TYPE_KEYSTORE:
		case TYPE_USERPASS_KEYSTORE:
			return getTemporaryPKCS12Password();

		case TYPE_PKCS12:
		case TYPE_USERPASS_PKCS12:
			return mPKCS12Password;


		case TYPE_USERPASS:
		case TYPE_STATICKEYS:
		case TYPE_CERTIFICATES:
		case TYPE_USERPASS_CERTIFICATES:
		default:
			return null;
		}
	}
	private boolean isUserPWAuth() {
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

	public String needUserPWInput() {
		if((mAuthenticationType == TYPE_PKCS12 || mAuthenticationType == TYPE_USERPASS_PKCS12)&&
				(mPKCS12Password.equals("") || mPKCS12Password == null)) {
			if(mTransientPCKS12PW==null)
				return "PKCS12 File Encryption Key";
		}
		if(isUserPWAuth() && (mPassword.equals("") || mPassword == null)) {
			if(mTransientPW==null)
				return "Password";

		}
		return null;
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



}




