/*
 * Copyright (c) 2012-2022 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import static de.blinkt.openvpn.core.OpenVpnManagementThread.ORBOT_TIMEOUT_MS;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.UdpPort;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import IPtProxy.IPtProxy;
import IPtProxy.PacketFlow;
import de.blinkt.openvpn.R;

public class TorProxy {

    private static final String TAG = "TorProxy";
    private static final int N_THREADS = 20;
    private static final DNSResolver dnsResolver = new DNSResolver(5400);
    private static ExecutorService executorService;
    private static PacketFlow pFlow;
    private static Handler resumeHandler;

    static {
        System.loadLibrary("torproxy");
    }

    private static void startSocks() {
        IPtProxy.startSocks(pFlow, "127.0.0.1", 9050);
        OrbotHelper.get().removeStatusCallback(statusCallback);
        setIsRunning(1);
    }

    public static native void processIncomingPackets(int inFD, int outFD);

    public static native void processOutgoingPackets(int inFD, int outFD);

    public static native void setIsRunning(int isRunning);

    public static native void writeToDevice(byte[] packet, int inFD);

    public static native void initTorPublicNodes(byte[][] nodes);

    public static ParcelFileDescriptor createProxy(ParcelFileDescriptor pfd, Context c) throws IOException {
        List<byte[]> torNodes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(c.getResources().openRawResource(R.raw.tor_nodes)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\\.");
                torNodes.add(new byte[]{(byte) Integer.parseInt(split[0]), (byte) Integer.parseInt(split[1]), (byte) Integer.parseInt(split[2]), (byte) Integer.parseInt(split[3])});
            }
        }
        initTorPublicNodes(torNodes.toArray(new byte[0][]));


        resumeHandler = new Handler(c.getMainLooper());
        executorService = Executors.newFixedThreadPool(N_THREADS);

        ParcelFileDescriptor[] pipe = ParcelFileDescriptor.createSocketPair();
        ParcelFileDescriptor vpnSide = pipe[0];
        ParcelFileDescriptor ourSide = pipe[1];

        setIsRunning(0);
        int inFD = pfd.detachFd();
        int outFD = ourSide.detachFd();
        executorService.execute(() -> processIncomingPackets(inFD, outFD));
        executorService.execute(() -> processOutgoingPackets(inFD, outFD));

        //write packets back out to TUN
        pFlow = packet -> {
            writeToDevice(packet, inFD);
        };

        OrbotHelper orbotHelper = OrbotHelper.get();
        if (!OrbotHelper.checkTorReceier(c)) {
            Log.e(TAG, "Orbot does not seem to be installed!");
        }

        resumeHandler.postDelayed(orbotStatusTimeOutRunnable, ORBOT_TIMEOUT_MS);
        orbotHelper.addStatusCallback(c, statusCallback);

        orbotHelper.sendOrbotStartAndStatusBroadcast();
        return vpnSide;
    }

    public static void closeResources() {
        setIsRunning(-1);
        IPtProxy.stopSocks();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        pFlow = null;
        resumeHandler = null;
    }

    public static void sendPacketsToTor(byte[] data) {
        try {
            Packet packet = IpSelector.newPacket(data, 0, data.length);
            if (packet instanceof IpPacket) {
                IpPacket ipPacket = (IpPacket) packet;
                if (isPacketDNS(ipPacket)) {
                    //We redirect dns packets through Orbot dns because we want to be able
                    //resolve .onion addresses
                    //Also using Tor + DNS of your VPN will decrease your privacy
                    executorService.execute(new RequestPacketHandler(ipPacket, pFlow, dnsResolver));
                } else {
                    IPtProxy.inputPacket(data);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "error reading from VPN fd: " + e.getLocalizedMessage());
        }
    }

    private static boolean isPacketDNS(IpPacket p) {
        if (p.getHeader().getProtocol() == IpNumber.UDP && p.getPayload() instanceof UdpPacket) {
            UdpPacket up = (UdpPacket) p.getPayload();
            return up.getHeader().getDstPort() == UdpPort.DOMAIN;
        }
        return false;
    }

    private static final Runnable orbotStatusTimeOutRunnable = TorProxy::startSocks;

    private static final OrbotHelper.StatusCallback statusCallback = new OrbotHelper.StatusCallback() {

        @Override
        public void onStatus(Intent statusIntent) {
            StringBuilder extras = new StringBuilder();
            for (String key : statusIntent.getExtras().keySet()) {
                Object val = statusIntent.getExtras().get(key);

                extras.append(String.format(Locale.ENGLISH, "%s - '%s'", key, val == null ? "null" : val.toString()));
            }
            Log.d(TAG, "Got Orbot status: " + extras);
        }

        @Override
        public void onNotYetInstalled() {
            Log.d(TAG, "Orbot not yet installed");
        }

        @Override
        public void onOrbotReady(Intent intent, String socksHost, int socksPort) {
            resumeHandler.removeCallbacks(orbotStatusTimeOutRunnable);
            startSocks();
        }

        @Override
        public void onDisabled(Intent intent) {
            Log.w(TAG, "Orbot integration for external applications is disabled. Waiting %ds before connecting to the default port. Enable external app integration in Orbot or use Socks v5 config instead of Orbot to avoid this delay.");
        }
    };

}
