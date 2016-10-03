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
}
