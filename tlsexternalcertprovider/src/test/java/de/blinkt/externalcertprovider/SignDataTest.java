/*
 * Copyright (c) 2012-2018 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.externalcertprovider;

import de.blinkt.openvpn.api.ExternalCertificateProvider;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class SignDataTest {
    @Test
    public void testSignData() throws Exception
    {
        SimpleSigner.signData(new byte[]{1,2,3,4,5,6,7,8}, false);
    }
}