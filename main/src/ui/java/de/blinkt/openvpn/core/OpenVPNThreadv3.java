package de.blinkt.openvpn.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import net.openvpn.ovpn3.ClientAPI_Config;
import net.openvpn.ovpn3.ClientAPI_EvalConfig;
import net.openvpn.ovpn3.ClientAPI_Event;
import net.openvpn.ovpn3.ClientAPI_ExternalPKICertRequest;
import net.openvpn.ovpn3.ClientAPI_ExternalPKISignRequest;
import net.openvpn.ovpn3.ClientAPI_LogInfo;
import net.openvpn.ovpn3.ClientAPI_OpenVPNClient;
import net.openvpn.ovpn3.ClientAPI_ProvideCreds;
import net.openvpn.ovpn3.ClientAPI_Status;
import net.openvpn.ovpn3.ClientAPI_TransportStats;

import java.util.Locale;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

import static de.blinkt.openvpn.VpnProfile.AUTH_RETRY_NOINTERACT;

public class OpenVPNThreadv3 extends ClientAPI_OpenVPNClient implements Runnable, OpenVPNManagement {

    final static long EmulateExcludeRoutes = (1 << 16);

    static {
        System.loadLibrary("ovpn3");
    }

    private VpnProfile mVp;
    private OpenVPNService mService;

    public OpenVPNThreadv3(OpenVPNService openVpnService, VpnProfile vp) {
        mVp = vp;
        mService = openVpnService;
    }

    @Override
    public void run() {
        String configstr = mVp.getConfigFile((Context) mService, true);
        if (!setConfig(configstr))
            return;
        setUserPW();
        VpnStatus.logInfo(platform());
        VpnStatus.logInfo(copyright());


        StatusPoller statuspoller = new StatusPoller(OpenVPNManagement.mBytecountInterval * 1000);
        new Thread(statuspoller, "Status Poller").start();

        ClientAPI_Status status = connect();
        if (status.getError()) {
            VpnStatus.logError(String.format("connect() error: %s: %s", status.getStatus(), status.getMessage()));
            VpnStatus.addExtraHints(status.getMessage());
        } else {
            VpnStatus.updateStateString("NOPROCESS", "OpenVPN3 thread finished", R.string.state_noprocess, ConnectionStatus.LEVEL_NOTCONNECTED);
        }
        statuspoller.stop();
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
    public boolean tun_builder_add_route(String address, int prefix_length, int metric, boolean ipv6) {
        if (address.equals("remote_host"))
            return false;

        if (ipv6)
            mService.addRoutev6(address + "/" + prefix_length, "tun");
        else
            mService.addRoute(new CIDRIP(address, prefix_length), true);
        return true;
    }

    @Override
    public boolean tun_builder_exclude_route(String address, int prefix_length, int metric, boolean ipv6) {
        if (ipv6)
            mService.addRoutev6(address + "/" + prefix_length, "wifi0");
        else {
            CIDRIP route = new CIDRIP(address, prefix_length);
            mService.addRoute(route, false);
        }
        return true;
    }

    @Override
    public boolean tun_builder_add_search_domain(String domain) {
        mService.setDomain(domain);
        return true;
    }

    @Override
    public boolean tun_builder_set_proxy_http(String host, int port)
    {
        return mService.addHttpProxy(host, port);
    }

    @Override
    public boolean tun_builder_set_proxy_https(String host, int port)
    {
        return false;
    }

    @Override
    public int tun_builder_establish() {
        return mService.openTun().detachFd();
    }

    @Override
    public boolean tun_builder_set_session_name(String name) {
        VpnStatus.logDebug("We should call this session" + name);
        return true;
    }

    @Override
    public boolean tun_builder_add_address(String address, int prefix_length, String gateway, boolean ipv6, boolean net30) {
        if (!ipv6)
            mService.setLocalIP(new CIDRIP(address, prefix_length));
        else
            mService.setLocalIPv6(address + "/" + prefix_length);
        return true;
    }

    @Override
    public boolean tun_builder_new() {

        return true;
    }

    @Override
    public boolean tun_builder_set_layer(int layer) {
        return layer == 3;
    }

    @Override
    public boolean tun_builder_reroute_gw(boolean ipv4, boolean ipv6, long flags) {
        if ((flags & EmulateExcludeRoutes) != 0)
            return true;
        if (ipv4)
            mService.addRoute("0.0.0.0", "0.0.0.0", "127.0.0.1", OpenVPNService.VPNSERVICE_TUN);

        if (ipv6)
            mService.addRoutev6("::/0", OpenVPNService.VPNSERVICE_TUN);

        return true;
    }

    private boolean setConfig(String vpnconfig) {

        ClientAPI_Config config = new ClientAPI_Config();
        if (mVp.getPasswordPrivateKey() != null)
            config.setPrivateKeyPassword(mVp.getPasswordPrivateKey());

        config.setContent(vpnconfig);
        config.setTunPersist(mVp.mPersistTun);
        config.setGuiVersion(VpnProfile.getVersionEnvString(mService));
        config.setSsoMethods("openurl,webauth,crtext");
        config.setPlatformVersion(mVp.getPlatformVersionEnvString());
        config.setExternalPkiAlias("extpki");
        config.setCompressionMode("asym");

        config.setHwAddrOverride(getFakeMacAddrFromSAAID(mService));
        config.setInfo(true);
        config.setAllowLocalLanAccess(mVp.mAllowLocalLAN);
        boolean retryOnAuthFailed = mVp.mAuthRetry == AUTH_RETRY_NOINTERACT;
        config.setRetryOnAuthFailed(retryOnAuthFailed);
        config.setEnableLegacyAlgorithms(mVp.mUseLegacyProvider);
        if (!TextUtils.isEmpty(mVp.mTlSCertProfile))
            config.setTlsCertProfileOverride(mVp.mTlSCertProfile);

        ClientAPI_EvalConfig ec = eval_config(config);
        if (ec.getExternalPki()) {
            VpnStatus.logDebug("OpenVPN3 core assumes an external PKI config");
        }
        if (ec.getError()) {
            VpnStatus.logError("OpenVPN config file parse error: " + ec.getMessage());
            return false;
        } else {
            config.setContent(vpnconfig);
            return true;
        }
    }

    @SuppressLint("HardwareIds")
    private String getFakeMacAddrFromSAAID(Context c) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

        String saaid = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        StringBuilder ret = new StringBuilder();
        if (saaid.length() >= 6) {
            byte[] sb = saaid.getBytes();
            for (int b = 0; b <= 6; b++) {
                if (b != 0)
                    ret.append(":");
                int v = sb[b] & 0xFF;
                ret.append(HEX_ARRAY[v >>> 4]);
                ret.append(HEX_ARRAY[v & 0x0F]);
            }
        }
        return ret.toString();
    }


    @Override
    public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest certreq) {
        VpnStatus.logDebug("Got external PKI certificate request from OpenVPN core");
        String[] ks = mVp.getExternalCertificates(mService);
        if (ks == null) {
            certreq.setError(true);
            certreq.setErrorText("Error in pki cert request");
            return;
        }

        String supcerts = ks[0];
        /* FIXME: How to differentiate between chain and ca certs in OpenVPN 3? */
        if (ks[1] != null)
            supcerts += "\n" + ks[1];
        certreq.setSupportingChain(supcerts);
        certreq.setCert(ks[2]);
        certreq.setError(false);
    }

    @Override
    public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest signreq) {
        VpnStatus.logDebug("Got external PKI signing request from OpenVPN core for algorithm " + signreq.getAlgorithm());
        SignaturePadding padding;
        switch (signreq.getAlgorithm()) {
            case "RSA_PKCS1_PADDING":
                padding = SignaturePadding.RSA_PKCS1_PADDING;
                break;
            case "RSA_NO_PADDING":
                padding = SignaturePadding.NO_PADDING;
                break;
            case "ECDSA":
                padding = SignaturePadding.NO_PADDING;
                break;
            default:
                throw new IllegalArgumentException("Illegal padding in sign request" + signreq.getAlgorithm());
        }
        signreq.setSig(mVp.getSignedData(mService, signreq.getData(), padding, "", "", false));
    }

    void setUserPW() {
        if (mVp.isUserPWAuth()) {
            ClientAPI_ProvideCreds creds = new ClientAPI_ProvideCreds();
            creds.setCachePassword(true);
            creds.setPassword(mVp.getPasswordAuth());
            creds.setUsername(mVp.mUsername);
            provide_creds(creds);
        }
    }

    @Override
    public boolean socket_protect(int socket, String remote, boolean ipv6) {
        return mService.protect(socket);

    }

    @Override
    public boolean stopVPN(boolean replaceConnection) {
        stop();
        return false;
    }

    @Override
    public void networkChange(boolean sameNetwork) {
        reconnect(1);
    }

    @Override
    public void setPauseCallback(PausedStateCallback callback) {
    }


    @Override
    public void sendCRResponse(String response) {
        post_cc_msg("CR_RESPONSE," + response + "\n");
    }

    @Override
    public void log(ClientAPI_LogInfo arg0) {
        String logmsg = arg0.getText();
        while (logmsg.endsWith("\n"))
            logmsg = logmsg.substring(0, logmsg.length() - 1);

        VpnStatus.logInfo(logmsg);
        VpnStatus.addExtraHints(logmsg);
    }

    @Override
    public void event(ClientAPI_Event event) {
        String name = event.getName();
        String info = event.getInfo();
        if (name.equals("INFO")) {
            if (info.startsWith("OPEN_URL:") || info.startsWith("CR_TEXT:")
                || info.startsWith("WEB_AUTH:")) {
                mService.trigger_sso(info);
            } else {
                VpnStatus.logInfo(R.string.info_from_server, info);
            }
        } else if (name.equals("COMPRESSION_ENABLED") || name.equals(("WARN"))) {
            VpnStatus.logInfo(String.format(Locale.US, "%s: %s", name, info));
        } else {
            VpnStatus.updateStateString(name, info);
        }
		/* if (event.name.equals("DYNAMIC_CHALLENGE")) {
			ClientAPI_DynamicChallenge challenge = new ClientAPI_DynamicChallenge();
			final boolean status = ClientAPI_OpenVPNClient.parse_dynamic_challenge(event.info, challenge);

		} else */
        if (event.getError())
            VpnStatus.logError(String.format("EVENT(Error): %s: %s", name, info));
    }

    @Override
    public net.openvpn.ovpn3.ClientAPI_StringVec tun_builder_get_local_networks(boolean ipv6) {

        net.openvpn.ovpn3.ClientAPI_StringVec nets = new net.openvpn.ovpn3.ClientAPI_StringVec();
        for (String net : NetworkUtils.getLocalNetworks(mService, ipv6))
            nets.add(net);
        return nets;
    }

    @Override
    public boolean pause_on_connection_timeout() {
        VpnStatus.logInfo("pause on connection timeout?! ");
        return true;
    }


    // When a connection is close to timeout, the core will call this
    // method.  If it returns false, the core will disconnect with a
    // CONNECTION_TIMEOUT event.  If true, the core will enter a PAUSE
    // state.

    @Override
    public void stop() {
        super.stop();
        mService.openvpnStopped();
    }

    @Override
    public void reconnect() {
        reconnect(1);
    }

    @Override
    public void pause(pauseReason reason) {
        super.pause(reason.toString());
    }

    class StatusPoller implements Runnable {
        boolean mStopped = false;
        private long mSleeptime;

        public StatusPoller(long sleeptime) {
            mSleeptime = sleeptime;
        }

        public void run() {
            while (!mStopped) {
                try {
                    Thread.sleep(mSleeptime);
                } catch (InterruptedException e) {
                }
                ClientAPI_TransportStats t = transport_stats();
                long in = t.getBytesIn();
                long out = t.getBytesOut();
                VpnStatus.updateByteCount(in, out);
            }
        }

        public void stop() {
            mStopped = true;
        }
    }


}
