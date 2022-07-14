/*
 * Copyright (c) 2012-2022 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static de.blinkt.openvpn.core.OpenVpnManagementThread.ORBOT_TIMEOUT_MS;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.pcap4j.packet.IllegalRawDataException;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpSelector;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.UdpPacket;
import org.pcap4j.packet.namednumber.IpNumber;
import org.pcap4j.packet.namednumber.UdpPort;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import IPtProxy.IPtProxy;
import IPtProxy.PacketFlow;

public class TorProxy {

    private static final String TAG = "TorProxy";
    private static final int N_THREADS = 100;
    private static final Map<Integer, String> KNOWN_UIDS = new ConcurrentHashMap<>();
    private static final DNSResolver dnsResolver = new DNSResolver(5400);
    private static ExecutorService executorService;
    private static ParcelFileDescriptor[] pipe;
    private static ParcelFileDescriptor vpnPfd;
    private static OutputStream outputDeviceStream;
    private static InputStream inputDeviceStream;
    private static InputStream inputOpenVPNStream;
    private static OutputStream outputOpenVPNStream;
    private static boolean isTorConnected;
    private static PacketFlow pFlow;
    private static Handler resumeHandler;

    private static void startSocks() {
        isTorConnected = true;
        //write packets back out to TUN
        pFlow = packet -> {
            try {
                outputDeviceStream.write(packet);
            } catch (IOException e) {
                Log.e(TAG, "error writing to VPN fd", e);
            }
        };

        IPtProxy.startSocks(pFlow, "127.0.0.1", 9050);
        OrbotHelper.get().removeStatusCallback(statusCallback);
    }

    public static ParcelFileDescriptor createProxy(ParcelFileDescriptor pfd, Context c) throws IOException {
        close();
        resumeHandler = new Handler(c.getMainLooper());
        executorService = Executors.newFixedThreadPool(N_THREADS);

        pipe = ParcelFileDescriptor.createSocketPair();
        vpnPfd = pfd;
        ParcelFileDescriptor vpnSide = pipe[0];
        ParcelFileDescriptor ourSide = pipe[1];
        outputDeviceStream = new DataOutputStream(new FileOutputStream(pfd.getFileDescriptor()));
        inputDeviceStream = new FileInputStream(pfd.getFileDescriptor());
        inputOpenVPNStream = new FileInputStream(ourSide.getFileDescriptor());
        outputOpenVPNStream = new FileOutputStream(ourSide.getFileDescriptor());

        executorService.execute(() -> processOutgoingPackets(c));

        executorService.execute(() -> processIncomingPackets(c));

        OrbotHelper orbotHelper = OrbotHelper.get();
        if (!OrbotHelper.checkTorReceier(c)) {
            Log.e(TAG, "Orbot does not seem to be installed!");
        }

        resumeHandler.postDelayed(orbotStatusTimeOutRunnable, ORBOT_TIMEOUT_MS);
        orbotHelper.addStatusCallback(c, statusCallback);

        orbotHelper.sendOrbotStartAndStatusBroadcast();

        return vpnSide;
    }

    public static void close() {
        IPtProxy.stopSocks();
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        if (pipe != null) {
            for (ParcelFileDescriptor pfd : pipe) {
                try {
                    pfd.close();
                } catch (Exception ignored) {

                }
            }
            pipe = null;
        }
        if (vpnPfd != null) {
            try {
                vpnPfd.close();
            } catch (Exception ignored) {

            }
            vpnPfd = null;
        }
        if (outputDeviceStream != null) {
            try {
                outputDeviceStream.close();
            } catch (Exception ignored) {

            }
            outputDeviceStream = null;
        }
        if (inputDeviceStream != null) {
            try {
                inputDeviceStream.close();
            } catch (Exception ignored) {

            }
            inputDeviceStream = null;
        }
        if (inputOpenVPNStream != null) {
            try {
                inputOpenVPNStream.close();
            } catch (Exception ignored) {

            }
            inputOpenVPNStream = null;
        }
        if (outputOpenVPNStream != null) {
            try {
                outputOpenVPNStream.close();
            } catch (Exception ignored) {

            }
            outputOpenVPNStream = null;
        }
        isTorConnected = false;
        pFlow = null;
        resumeHandler = null;
    }

    private static void processIncomingPackets(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(CONNECTIVITY_SERVICE);
        PackageManager pm = c.getPackageManager();
        byte[] buf = new byte[16384];
        while (!Thread.interrupted()) {
            try {
                int read = inputOpenVPNStream.read(buf);
                if (read > 0) {
                    int protocol = getProtocol(buf);
                    if (protocol == 6 || protocol == 17) {
                        InetSocketAddress sourceAddress = getSourceAddress(buf, protocol);
                        InetSocketAddress destinationAddress = getDestinationAddress(buf, protocol);
                        int uid = cm.getConnectionOwnerUid(protocol, destinationAddress, sourceAddress);
                        String appName = KNOWN_UIDS.computeIfAbsent(uid, pm::getNameForUid);
                        if (OpenVPNService.ORBOT_PACKAGE_NAME.equals(appName)) {
                            outputDeviceStream.write(buf, 0, read);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while transferring packets from device to vpn", e);
            }
        }
    }

    private static void sendPacketsToTor(byte[] buf, int read) {
        try {
            if (read > 0) {
                byte[] data = Arrays.copyOf(buf, read);
                try {
                    Packet packet = IpSelector.newPacket(data, 0, data.length);
                    if (packet instanceof IpPacket) {
                        IpPacket ipPacket = (IpPacket) packet;
                        if (isPacketDNS(ipPacket)) {
                            executorService.execute(new RequestPacketHandler(ipPacket, pFlow, dnsResolver));
                        } else {
                            IPtProxy.inputPacket(data);
                        }
                    }
                } catch (IllegalRawDataException e) {
                    Log.e(TAG, e.getLocalizedMessage());
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "error reading from VPN fd: " + e.getLocalizedMessage());
        }
    }

    private static boolean isPacketDNS(IpPacket p) {
        if (p.getHeader().getProtocol() == IpNumber.UDP) {
            UdpPacket up = (UdpPacket) p.getPayload();
            return up.getHeader().getDstPort() == UdpPort.DOMAIN;
        }
        return false;
    }

    private static void processOutgoingPackets(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(CONNECTIVITY_SERVICE);
        PackageManager pm = c.getPackageManager();
        byte[] buf = new byte[16384];
        while (!Thread.interrupted()) {
            try {
                int read = inputDeviceStream.read(buf);
                if (read > 0) {
                    int protocol = getProtocol(buf);
                    if (protocol == 6 || protocol == 17) {
                        InetSocketAddress sourceAddress = getSourceAddress(buf, protocol);
                        InetSocketAddress destinationAddress = getDestinationAddress(buf, protocol);
                        int uid = cm.getConnectionOwnerUid(protocol, sourceAddress, destinationAddress);
                        String appName = KNOWN_UIDS.computeIfAbsent(uid, pm::getNameForUid);
                        if (OpenVPNService.ORBOT_PACKAGE_NAME.equals(appName)) {
                            outputOpenVPNStream.write(buf, 0, read);
                            outputOpenVPNStream.flush();
                            continue;
                        }
                    }
                }
                if (isTorConnected) {
                    sendPacketsToTor(buf, read);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while transferring packets from device to vpn", e);
            }
        }
    }

    private static int getProtocol(byte[] buf) {
        return Byte.toUnsignedInt(buf[9]);
    }

    private static InetSocketAddress getSourceAddress(byte[] buf, int protocol) throws UnknownHostException {
        int length = buf[0] & 0x0F;
        int srcPort;
        if (protocol == 6 || protocol == 17) {
            srcPort = Byte.toUnsignedInt(buf[length * 4]) << 8 | Byte.toUnsignedInt(buf[length * 4 + 1]);
        } else {
            return null;
        }
        byte[] srcAddressBytes = Arrays.copyOfRange(buf, 12, 16);
        return new InetSocketAddress(InetAddress.getByAddress(srcAddressBytes), srcPort);
    }

    private static InetSocketAddress getDestinationAddress(byte[] buf, int protocol) throws UnknownHostException {
        int length = buf[0] & 0x0F;
        int dstPort;
        if (protocol == 6 || protocol == 17) {
            dstPort = Byte.toUnsignedInt(buf[length * 4 + 2]) << 8 | Byte.toUnsignedInt(buf[length * 4 + 3]);
        } else {
            return null;
        }
        byte[] dstAddressBytes = Arrays.copyOfRange(buf, 16, 20);
        return new InetSocketAddress(InetAddress.getByAddress(dstAddressBytes), dstPort);
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
