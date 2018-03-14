/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import de.blinkt.openvpn.VpnProfile;

/**
 * Created by arne on 03.10.16.
 */

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
        Assert.assertTrue(p.mCustomConfigOptions.contains(httpproxypass));

    }

    @Test
    public void testSockProxyImport() throws IOException, ConfigParser.ConfigParseError {
        String proxy =
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
    }


}
