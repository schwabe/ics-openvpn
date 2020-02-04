/* 
 PersonalDNSFilter 1.5
 Copyright (C) 2017 Ingo Zenz

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 Find the latest version at http://www.zenz-solutions.de/personaldnsfilter
 Contact:i.z@gmx.net 
 */

package de.blinkt.openvpn.core.capture;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.capture.ip.IPPacket;
import de.blinkt.openvpn.core.capture.ip.UDPPacket;


public class DNSResolver implements Runnable {

	private static int TIMEOUT = 15000;

	private static int THR_COUNT = 0;
	private static Object CNT_SYNC = new Object();
	private static boolean IO_ERROR=false;

	//for android usage based on IP packages from the VPN Interface
	private UDPPacket udpRequestPacket;
	private OutputStream responseOut;
	private InetAddress dnsAddress;
	private int dnsPort;



	private boolean datagramPacketMode = false;

	public DNSResolver(UDPPacket udpRequestPacket, OutputStream reponseOut, InetAddress adr, int port) {

		this.udpRequestPacket = udpRequestPacket;
		this.responseOut = reponseOut;
		this.dnsAddress=adr;
		this.dnsPort= port;
	}


	public void resolve(DatagramPacket request, DatagramPacket response) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		try {
			request.setAddress(dnsAddress);
			request.setPort(dnsPort);
			socket.setSoTimeout(15000);
			try {
				socket.send(request);
			} catch (IOException eio) {
				throw new IOException("Cannot reach " + dnsAddress+":"+dnsPort + "!" + eio.getMessage());
			}
			try {
				socket.receive(response);
			} catch (IOException eio) {
				throw new IOException("No DNS Response from " + dnsAddress+":"+dnsPort);
			}
		} finally {
			socket.close();
		}
	}

	private void processIPPackageMode() throws Exception {
		int ttl = udpRequestPacket.getTTL();
		int[] sourceIP = udpRequestPacket.getSourceIP();
		int[] destIP = udpRequestPacket.getDestIP();
		int sourcePort = udpRequestPacket.getSourcePort();
		int destPort = udpRequestPacket.getDestPort();
		int version = udpRequestPacket.getVersion();


		int hdrLen = udpRequestPacket.getHeaderLength();
		byte[] packetData = udpRequestPacket.getData();
		int ipOffs = udpRequestPacket.getIPPacketOffset();
		int offs = ipOffs + hdrLen;
		int len = udpRequestPacket.getIPPacketLength() - hdrLen;

		// build request datagram packet from UDP request packet
		DatagramPacket request = new DatagramPacket(packetData, offs, len);

		// we can reuse the request data array
		DatagramPacket response = new DatagramPacket(packetData, offs, packetData.length - offs);

		//forward request to DNS and receive response
		resolve(request, response);

		//create  UDP Header and update source and destination IP and port
		UDPPacket udp = UDPPacket.createUDPPacket(response.getData(), ipOffs, hdrLen + response.getLength(), version);

		//for the response source and destination have to be switched
		udp.updateHeader(ttl, 17, destIP, sourceIP);
		udp.updateHeader(destPort, sourcePort);

		//finally return the response packet
		synchronized (responseOut) {
			responseOut.write(udp.getData(), udp.getIPPacketOffset(), udp.getIPPacketLength());
			responseOut.flush();
		}
	}


	@Override
	public void run() {
		try {
			synchronized (CNT_SYNC) {
				THR_COUNT++;
			}
			processIPPackageMode();

			IO_ERROR=false;

		} catch (IOException e) {
			VpnStatus.logWarning(e.toString());

		} catch (Exception e) {
			VpnStatus.logException(e);
		} finally {
			synchronized (CNT_SYNC) {
				THR_COUNT--;
			}
		}
	}

	public static int getResolverCount() {
		return THR_COUNT;
	}

}
