package de.blinkt.openvpn;

import net.openvpn.ovpn3.ClientAPI_Config;
import net.openvpn.ovpn3.ClientAPI_EvalConfig;
import net.openvpn.ovpn3.ClientAPI_Event;
import net.openvpn.ovpn3.ClientAPI_ExternalPKICertRequest;
import net.openvpn.ovpn3.ClientAPI_ExternalPKISignRequest;
import net.openvpn.ovpn3.ClientAPI_LogInfo;
import net.openvpn.ovpn3.ClientAPI_OpenVPNClient;
import net.openvpn.ovpn3.ClientAPI_ProvideCreds;
import net.openvpn.ovpn3.ClientAPI_Status;

public class OpenVPNThreadv3 extends ClientAPI_OpenVPNClient implements Runnable {

	static {
		System.loadLibrary("crypto");
		System.loadLibrary("ssl");
		System.loadLibrary("ovpn3");
	}

	private VpnProfile mVp;
	private OpenVpnService mService;

	@Override
	public void run() {
		String configstr = mVp.getConfigFile(mService,true); 
		if(!setConfig(configstr))
			return;
		setUserPW();
		OpenVPN.logInfo(copyright());
		ClientAPI_Status status = connect();
		if(status.getError()) {
			OpenVPN.logError(String.format("connect() error: %s: %s",status.getStatus(),status.getMessage()));
		} else {
			OpenVPN.logInfo(String.format("connect() error: %s: %s",status.getStatus(),status.getMessage()));
		}
	}
	
	@Override
	public boolean tun_builder_set_remote_address(String address, boolean ipv6) {
		mService.setMtu(1500);
		return true;
	}
	
	@Override
	public boolean tun_builder_set_mtu(int mtu) {
		mService.setMtu(mtu);
		return true;
	}
	@Override
	public boolean tun_builder_add_dns_server(String address, boolean ipv6) {
		mService.addDNS(address);
		return true;
	}
	
	@Override
	public boolean tun_builder_add_route(String address, int prefix_length,
			boolean ipv6) {
		if(ipv6)
			mService.addRoutev6(address + "/" + prefix_length);
		else
			mService.addRoute(new CIDRIP(address, prefix_length));
		return true;
	}
	
	@Override
	public boolean tun_builder_add_search_domain(String domain) {
		mService.setDomain(domain);
		return true;
	}
	
	@Override
	public int tun_builder_establish() {
		return mService.openTun().detachFd();
	}
	
	@Override
	public boolean tun_builder_set_session_name(String name) {
		OpenVPN.logInfo("We should call this session" + name);
		return true;
	}
	

	
	@Override
	public boolean tun_builder_add_address(String address, int prefix_length,
			boolean ipv6) {
		if(!ipv6)
			mService.setLocalIP(new CIDRIP(address, prefix_length));
		else
			mService.setLocalIPv6(address+ "/" + prefix_length);
		return true;
	}
	
	@Override
	public boolean tun_builder_new() {
		
		return true;
	}
	
	@Override
	public boolean tun_builder_reroute_gw(String server_address,
			boolean server_address_ipv6, boolean ipv4, boolean ipv6, long flags) {
		// ignore
		return true;
	}
	
	@Override
	public boolean tun_builder_exclude_route(String address, int prefix_length,
			boolean ipv6) {
		//ignore
		return true;
	}


	private boolean setConfig(String vpnconfig) {
		
		ClientAPI_Config config = new ClientAPI_Config();
		if(mVp.getPasswordPrivateKey()!=null)
			config.setPrivateKeyPassword(mVp.getPasswordPrivateKey());
		
		config.setContent(vpnconfig);
		config.setTunPersist(mVp.mPersistTun);
		config.setExternalPkiAlias("extpki");
		
		ClientAPI_EvalConfig ec = eval_config(config);
		if(ec.getExternalPki()) {
			OpenVPN.logError("OpenVPN seem to think as external PKI");
		}
		if (ec.getError()) {
			OpenVPN.logError("OpenVPN config file parse error: " + ec.getMessage());
			return false;
		} else {
			config.setContent(vpnconfig);
			return true;
		}
	}
	
	@Override
	public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest certreq) {
		OpenVPN.logError("EXT PKI CERT");
		String[] ks = mVp.getKeyStoreCertificates(mService);
		if(ks==null) {
			certreq.setError(true);
			certreq.setErrorText("Error in pki cert request");
			return;
		}
			
		certreq.setSupportingChain(ks[0]);
		certreq.setCert(ks[1]);
		certreq.setError(false);
	}
	
	@Override
	public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest signreq) {
		OpenVPN.logError("EXT PKI Sign");
		signreq.setSig(mVp.getSignedData(signreq.getData()));
	}

	void setUserPW() {
		if(mVp.isUserPWAuth()) {
			ClientAPI_ProvideCreds creds = new ClientAPI_ProvideCreds();
			creds.setCachePassword(true);
			creds.setPassword(mVp.getPasswordAuth());
			creds.setUsername(mVp.mUsername);
			provide_creds(creds);
		}
	}

	@Override
	public boolean socket_protect(int socket) {
		boolean b= mService.protect(socket);
		OpenVPN.logInfo("protect from v3: " + b);
		return true;
		
	}

	public OpenVPNThreadv3(OpenVpnService openVpnService, VpnProfile vp) {
		init_process();
		mVp =vp;
		mService =openVpnService;
	}

	@Override
	public void log(ClientAPI_LogInfo arg0) {
		String logmsg =arg0.getText();
		while (logmsg.endsWith("\n"))
			logmsg = logmsg.substring(0, logmsg.length()-1);
		
		OpenVPN.logInfo(logmsg);
	}

	@Override
	public void event(ClientAPI_Event arg0) {
		if(arg0.getError())
			OpenVPN.logError(String.format("EVENT(Error): %s: %s",arg0.getName(),arg0.getInfo()));
		else
			OpenVPN.logInfo(String.format("EVENT %s: %s",arg0.getName(),arg0.getInfo()));
	}

}
