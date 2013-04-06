// IOpenVPNAPIService.aidl
package de.blinkt.openvpn.api;

import de.blinkt.openvpn.api.APIVpnProfile;
import de.blinkt.openvpn.api.IOpenVPNStatusCallback; 

import android.content.Intent;
import android.os.ParcelFileDescriptor;

interface IOpenVPNAPIService {
	List<APIVpnProfile> getProfiles();
	
	void startProfile (String profileUUID);
	
	/* Use a profile with all certificates etc. embedded */
	boolean addVPNProfile (String name, String config);
	
	/* start a profile using an config */
	void startVPN (String inlineconfig);
	
	/* This permission framework is used  to avoid confused deputy style attack to the VPN
	 * calling this will give null if the app is allowed to use the frame and null otherwise */
	Intent prepare (String packagename);
	
	/* Tells the calling app wether we already have permission to avoid calling the activity/flicker */
	boolean hasPermission();

	/* Disconnect the VPN */
    void disconnect();
    
    /**
      * Registers to receive OpenVPN Status Updates
      */
    void registerStatusCallback(IOpenVPNStatusCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterStatusCallback(IOpenVPNStatusCallback cb);
		
}