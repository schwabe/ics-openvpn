package de.blinkt.openvpn;

import java.io.Serializable;
import java.util.UUID;

import android.os.Parcel;
import android.os.Parcelable;

public class VpnProfile implements Parcelable, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7085688938959334563L;
	static final int TYPE_CERTIFICATES=0;
	static final int TYPE_PKCS12=1;
	static final int TYPE_KEYSTORE=2;
	public static final int TYPE_USERPASS = 3;
	public static final int TYPE_STATICKEYS = 4;


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


	

}
