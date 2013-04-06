package de.blinkt.openvpn.api;

import android.os.Parcel;
import android.os.Parcelable;

public class APIVpnProfile implements Parcelable {

	public final String mUUID;
	public final String mName;
	public final boolean mUserEditable;

	

	public APIVpnProfile(Parcel in) {
		mUUID = in.readString();
		mName = in.readString();
		if(in.readInt()==0)
			mUserEditable=false;
		else
			mUserEditable=true;
	}

	public APIVpnProfile(String uuidString, String name, boolean userEditable) {
		mUUID=uuidString;
		mName = name;
		mUserEditable=userEditable;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mUUID);
		dest.writeString(mName);
		if(mUserEditable)
			dest.writeInt(0);
		else
			dest.writeInt(1);
	}

	public static final Parcelable.Creator<APIVpnProfile> CREATOR
	= new Parcelable.Creator<APIVpnProfile>() {
		public APIVpnProfile createFromParcel(Parcel in) {
			return new APIVpnProfile(in);
		}

		public APIVpnProfile[] newArray(int size) {
			return new APIVpnProfile[size];
		}
	};


}
