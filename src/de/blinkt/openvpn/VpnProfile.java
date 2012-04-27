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
import java.util.Random;
import java.util.UUID;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
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
	
	private static final String OVPNCONFIGFILE = "android.conf";

	// Keep in order of parceling
	// Public attributes, since I got mad with getter/setter
	// set members to default values
	private UUID mUuid;
	public int mAuthenticationType = TYPE_KEYSTORE ;
	public String mName;
	public String mAlias;
	public String mClientCertFilename;
	public int mTLSAuthDirection=2;
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
	public String mDNS1="131.234.137.23";
	public String mDNS2="131.234.137.24";
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
	private String mPassword;
	private String mUsername;


	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeInt(mAuthenticationType);
		out.writeLong(mUuid.getMostSignificantBits());
		out.writeLong(mUuid.getLeastSignificantBits());
		out.writeString(mName);
		out.writeString(mAlias);
		out.writeString(mClientCertFilename);
		out.writeInt(mTLSAuthDirection);
		out.writeString(mTLSAuthFilename);
		out.writeString(mClientKeyFilename);
		out.writeString(mCaFilename);
		out.writeValue(mUseLzo);
		out.writeString(mServerPort);
		out.writeValue(mUseUdp);
		out.writeString(mPKCS12Filename);
		out.writeString(mPKCS12Password);
		out.writeValue(mUseTLSAuth);
		out.writeString(mServerName);
	}

	private VpnProfile(Parcel in) {
		mAuthenticationType = in.readInt();
		mUuid = new UUID(in.readLong(), in.readLong());
		mName = in.readString();
		mAlias = in.readString();
		mClientCertFilename = in.readString();
		mTLSAuthDirection = in.readInt(); 
		mTLSAuthFilename = in.readString();
		mClientKeyFilename = in.readString();
		mCaFilename = in.readString();
		mUseLzo = (Boolean) in.readValue(null);
		mServerPort = in.readString();
		mUseUdp = (Boolean) in.readValue(null);
		mPKCS12Filename = in.readString();
		mPKCS12Password = in.readString();
		mUseTLSAuth = (Boolean) in.readValue(null);
		mServerName = in.readString(); 
	}

	public static final Parcelable.Creator<VpnProfile> CREATOR
	= new Parcelable.Creator<VpnProfile>() {
		public VpnProfile createFromParcel(Parcel in) {
			return new VpnProfile(in);
		}

		public VpnProfile[] newArray(int size) {
			return new VpnProfile[size];
		}
	};


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
		
		
		// TODO  "--remote-cert-eku", "TLS Web Server Authentication"


		
		cfg+="client\n";
		cfg+="verb 2\n";


		// /tmp does not exist on Android
		cfg+="tmp-dir ";
		cfg+=cacheDir.getAbsolutePath();
		cfg+="\n";

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
		case VpnProfile.TYPE_CERTIFICATES:
			// Ca
			cfg+="ca ";
			cfg+=mCaFilename;
			cfg+="\n";

			// Client Cert + Key
			cfg+="key ";
			cfg+=mClientKeyFilename;
			cfg+="\n";
			cfg+="cert ";
			cfg+=mClientCertFilename;
			cfg+="\n";
			break;
		case VpnProfile.TYPE_PKCS12:
			cfg+="pkcs12 ";
			cfg+=mPKCS12Filename;
			cfg+="\n";
			cfg+="management-query-passwords\n";
			break;

		case VpnProfile.TYPE_KEYSTORE:
			cfg+="pkcs12 ";
			cfg+=cacheDir.getAbsolutePath() + "/" + OVPNCONFIGPKCS12;
			cfg+="\n";
			cfg+="management-query-passwords\n";
			break;

		}

		if(mUseLzo) {
			cfg+="comp-lzo\n";
		}

		if(mUseTLSAuth) {
			cfg+="tls-auth ";
			cfg+=mTLSAuthFilename;
			int tlsdir= mTLSAuthDirection;
			// 2 is unspecified
			if(tlsdir == 0 || tlsdir==1) {
				cfg+=" ";
				cfg+=new Integer(tlsdir).toString();
			}
			cfg+="\n";
		}
		
		return cfg;
	}
	
	private String[] buildOpenvpnArgv(File cacheDir)
	{
		Vector<String> args = new Vector<String>();

		// Add fixed paramenters
		args.add("openvpn");

		// Enable managment interface to 
		// stop openvpn
		args.add("--management");

		args.add(cacheDir.getAbsolutePath() + "/" +  "mgmtsocket");
		args.add("unix");
		//args.add("--management-hold");

		args.add("--config");
		args.add(cacheDir.getAbsolutePath() + "/" + OVPNCONFIGFILE);


		return  (String[]) args.toArray(new String[args.size()]);
	}

	public Intent prepareIntent(Activity activity) {
		String prefix = activity.getPackageName();
		
		 Intent intent = new Intent(activity,OpenVpnService.class);

		intent.putExtra(prefix + ".ARGV" , buildOpenvpnArgv(activity.getCacheDir()));

		if(mAuthenticationType == TYPE_PKCS12){
			intent.putExtra(prefix + ".PKCS12PASS",
					mPKCS12Password);
		}

		if(mAuthenticationType == VpnProfile.TYPE_KEYSTORE) {
			String pkcs12pw = savePKCS12(activity);
			intent.putExtra(prefix + ".PKCS12PASS", pkcs12pw);
		}
		
		if(mAuthenticationType == VpnProfile.TYPE_USERPASS) {
			intent.putExtra(prefix + ".USERNAME", mUsername);
			intent.putExtra(prefix + ".PASSWORD", mPassword);
		}
		
		try {
			FileWriter cfg = new FileWriter(activity.getCacheDir().getAbsolutePath() + "/" + OVPNCONFIGFILE);
			cfg.write(getConfigFile(activity.getCacheDir()));
			cfg.flush();
			cfg.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return intent;
	}
	
	private String getRandomPW() {
		String pw= "";
		// Put enough digits togher to make a password :)
		Random r = new Random();
		for(int i=0;i < 4;i++) {
			pw += new Integer(r.nextInt(1000)).toString();
		}

		return pw;

	}

	private String savePKCS12(Context context) {
		PrivateKey privateKey = null;
		X509Certificate[] cachain=null;
		try {
			privateKey = KeyChain.getPrivateKey(context,mAlias);
			cachain = KeyChain.getCertificateChain(context, mAlias);

			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(null, null);
			ks.setKeyEntry("usercert", privateKey, null, cachain);
			String mypw = getRandomPW();
			FileOutputStream fout = new FileOutputStream(context.getCacheDir().getAbsolutePath() + "/" + VpnProfile.OVPNCONFIGPKCS12);
			ks.store(fout,mypw.toCharArray());
			fout.flush(); fout.close();
			return mypw;
		} catch (KeyChainException e) {
			e.printStackTrace();
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
		}
		return "ERROR";

	}


}




