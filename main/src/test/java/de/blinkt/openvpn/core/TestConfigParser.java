/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.app.Application;
import android.content.Context;
import de.blinkt.openvpn.R;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;
import org.robolectric.annotation.Config;

/**
 * Created by arne on 03.10.16.
 */

@Config(manifest= "src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class TestConfigParser {

    String miniconfig = "client\nremote test.blinkt.de\n";

    @Test
    public void testHttpProxyPass() throws IOException, ConfigParser.ConfigParseError {
        String httpproxypass = "<http-proxy-user-pass>\n" +
                "foo\n" +
                "bar\n" +
                "</http-proxy-user-pass>\n";

        ConfigParser cp = new ConfigParser();
        cp.parseConfig(new StringReader(miniconfig + httpproxypass));
        VpnProfile p = cp.convertProfile();
        Assert.assertFalse(p.mCustomConfigOptions.contains(httpproxypass));


    }

    @Test
    public void testSockProxyImport() throws IOException, ConfigParser.ConfigParseError {
        String proxy =
                "ca baz\n" +
                "key foo\n" +
                "cert bar\n" +
                        "client\n" +
                        "<connection>\n" +
                        "socks-proxy 13.23.3.2\n" +
                        "remote foo.bar\n" +
                        "</connection>\n" +
                        "\n" +
                        "<connection>\n" +
                        "socks-proxy 1.2.3.4 1234\n" +
                        "remote foo.bar\n" +
                        "</connection>\n" +
                        "\n" +
                        "<connection>\n" +
                        "http-proxy 1.2.3.7 8080\n" +
                        "remote foo.bar\n" +
                        "</connection>";

        ConfigParser cp = new ConfigParser();
        cp.parseConfig(new StringReader(proxy));
        VpnProfile vp = cp.convertProfile();
        Assert.assertEquals(3, vp.mConnections.length);

        Assert.assertEquals("13.23.3.2", vp.mConnections[0].mProxyName);
        Assert.assertEquals("1080", vp.mConnections[0].mProxyPort);
        Assert.assertEquals(Connection.ProxyType.SOCKS5, vp.mConnections[0].mProxyType);

        Assert.assertEquals("1.2.3.4", vp.mConnections[1].mProxyName);
        Assert.assertEquals("1234", vp.mConnections[1].mProxyPort);
        Assert.assertEquals(Connection.ProxyType.SOCKS5, vp.mConnections[0].mProxyType);

        Assert.assertEquals("1.2.3.7", vp.mConnections[2].mProxyName);
        Assert.assertEquals("8080", vp.mConnections[2].mProxyPort);
        Assert.assertEquals(Connection.ProxyType.HTTP, vp.mConnections[2].mProxyType);

        Context c = RuntimeEnvironment.application;
        int err = vp.checkProfile(c, false);
        Assert.assertTrue("Failed with " + c.getString(err), err == R.string.no_error_found);
    }

    @Test
    public void testHttpUserPassAuth() throws IOException, ConfigParser.ConfigParseError {
        String proxy ="client\n" +
                "dev tun\n" +
                "proto tcp\n" +
                "remote 1.2.3.4 443\n" +
                "resolv-retry infinite\n" +
                "nobind\n" +
                "persist-key\n" +
                "persist-tun\n" +
                "auth-user-pass\n" +
                "verb 3\n" +
                "cipher AES-128-CBC\n" +
                "pull\n" +
                "route-delay 2\n" +
                "redirect-gateway\n" +
                "remote-cert-tls server\n" +
                "ns-cert-type server\n" +
                "comp-lzo no\n" +
                "http-proxy 1.2.3.4 1234\n" +
                "<http-proxy-user-pass>\n" +
                "username12\n" +
                "password34\n" +
                "</http-proxy-user-pass>\n" +
                "<ca>\n" +
                "foo\n" +
                "</ca>\n" +
                "<cert>\n" +
                "bar\n" +
                "</cert>\n" +
                "<key>\n" +
                "baz\n" +
                "</key>\n";
        ConfigParser cp = new ConfigParser();
        cp.parseConfig(new StringReader(proxy));
        VpnProfile vp = cp.convertProfile();
        String config = vp.getConfigFile(RuntimeEnvironment.application, true);
        Assert.assertTrue(config.contains("username12"));
        Assert.assertTrue(config.contains("http-proxy 1.2.3.4"));

         config = vp.getConfigFile(RuntimeEnvironment.application, false);

        Assert.assertFalse(config.contains("username12"));
        Assert.assertFalse(config.contains("http-proxy 1.2.3.4"));

        Assert.assertTrue(vp.mConnections[0].mUseProxyAuth);
        Assert.assertEquals(vp.mConnections[0].mProxyAuthUser, "username12");
        Assert.assertEquals(vp.mConnections[0].mProxyAuthPassword, "password34");
    }

}
