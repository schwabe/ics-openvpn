/*
 * Copyright (c) 2012-2022 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.net.TrafficStats;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DNSResolver {

    private final int mPort;
    private InetAddress mLocalhost = null;

    public DNSResolver(int localPort) {
        mPort = localPort;
    }

    public byte[] processDNS(byte[] payload) throws IOException {
        // TODO: HACK
        int THREAD_ID = 10000;
        TrafficStats.setThreadStatsTag(THREAD_ID);
        if (mLocalhost == null) {
            mLocalhost = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        }

        DatagramPacket packet = new DatagramPacket(
                payload, payload.length, mLocalhost, mPort
        );
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.send(packet);

            // Await response from DNS server
            byte[] buf = new byte[1024];
            packet = new DatagramPacket(buf, buf.length);
            datagramSocket.receive(packet);
        }

        return packet.getData();
    }

}
