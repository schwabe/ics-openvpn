/*
 * Copyright (c) 2012-2022 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.util.Log;

import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.IpV4Packet;
import org.pcap4j.packet.IpV6Packet;
import org.pcap4j.packet.UdpPacket;

import java.net.Inet6Address;

import IPtProxy.PacketFlow;


public class RequestPacketHandler implements Runnable {

    final IpPacket packet;
    final PacketFlow pFlow;
    final DNSResolver mDnsResolver;

    private static final String TAG = "RequestPacketHandler";

    public RequestPacketHandler (IpPacket packet, PacketFlow pFLow, DNSResolver dnsResolver) {
        this.packet = packet;
        this.pFlow = pFLow;
        this.mDnsResolver = dnsResolver;
    }
    public void run() {
            try {
                UdpPacket udpPacket = (UdpPacket) packet.getPayload();

                byte[] dnsResp = mDnsResolver.processDNS(udpPacket.getPayload().getRawData());

                if (dnsResp != null) {

                     DnsPacket dnsRequest = (DnsPacket) udpPacket.getPayload();
                     DnsPacket dnsResponse = DnsPacket.newPacket(dnsResp, 0, dnsResp.length);

                     DnsPacket.Builder dnsBuilder = new DnsPacket.Builder();
                     dnsBuilder.questions(dnsRequest.getHeader().getQuestions());
                     dnsBuilder.id(dnsRequest.getHeader().getId());

                     dnsBuilder.answers(dnsResponse.getHeader().getAnswers());
                     dnsBuilder.response(dnsResponse.getHeader().isResponse());
                     dnsBuilder.additionalInfo(dnsResponse.getHeader().getAdditionalInfo());
                     dnsBuilder.qdCount(dnsResponse.getHeader().getQdCount());
                     dnsBuilder.anCount(dnsResponse.getHeader().getAnCount());
                     dnsBuilder.arCount(dnsResponse.getHeader().getArCount());
                     dnsBuilder.opCode(dnsResponse.getHeader().getOpCode());
                     dnsBuilder.rCode(dnsResponse.getHeader().getrCode());
                     dnsBuilder.authenticData(dnsResponse.getHeader().isAuthenticData());
                     dnsBuilder.authoritativeAnswer(dnsResponse.getHeader().isAuthoritativeAnswer());
                     dnsBuilder.authorities(dnsResponse.getHeader().getAuthorities());

                    UdpPacket.Builder udpBuilder = new UdpPacket.Builder(udpPacket)
                            .srcPort(udpPacket.getHeader().getDstPort())
                            .dstPort(udpPacket.getHeader().getSrcPort())
                            .srcAddr(packet.getHeader().getDstAddr())
                            .dstAddr(packet.getHeader().getSrcAddr())
                            .correctChecksumAtBuild(true)
                            .correctLengthAtBuild(true)
                            .payloadBuilder(dnsBuilder);

                    IpPacket respPacket = null;

                    if (packet instanceof IpV4Packet) {

                        IpV4Packet ipPacket = (IpV4Packet)packet;
                        IpV4Packet.Builder ipv4Builder = new IpV4Packet.Builder();
                        ipv4Builder
                                .version(ipPacket.getHeader().getVersion())
                                .protocol(ipPacket.getHeader().getProtocol())
                                .tos(ipPacket.getHeader().getTos())
                                .srcAddr(ipPacket.getHeader().getDstAddr())
                                .dstAddr(ipPacket.getHeader().getSrcAddr())
                                .correctChecksumAtBuild(true)
                                .correctLengthAtBuild(true)
                                .dontFragmentFlag(ipPacket.getHeader().getDontFragmentFlag())
                                .reservedFlag(ipPacket.getHeader().getReservedFlag())
                                .moreFragmentFlag(ipPacket.getHeader().getMoreFragmentFlag())
                                .ttl(Integer.valueOf(64).byteValue())
                                .payloadBuilder(udpBuilder);

                        respPacket = ipv4Builder.build();

                    }
                    else if (packet instanceof IpV6Packet) {
                        respPacket = new IpV6Packet.Builder((IpV6Packet) packet)
                                .srcAddr((Inet6Address) packet.getHeader().getDstAddr())
                                .dstAddr((Inet6Address) packet.getHeader().getSrcAddr())
                                .payloadBuilder(udpBuilder)
                                .build();
                    }


                    byte[] rawResponse = respPacket.getRawData();
                    pFlow.writePacket(rawResponse);
                }

            } catch (Exception ioe) {
                Log.e(TAG, "could not parse DNS packet: " + ioe);
            }
    }
}
