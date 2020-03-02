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

package de.blinkt.openvpn.core.capture.ip;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;


public class UDPPacket extends IPPacket {

	private IntBuffer udpHeader;

	public UDPPacket(byte[] packet, int offs, int len) {
		super(packet, offs, len);
		this.udpHeader = ByteBuffer.wrap(packet, offs + ipHdrlen, 8).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
	}


	public static UDPPacket createUDPPacket(byte[] packet, int offs, int len, int version) {
		packet[offs] = ((byte) (version << 4 & 0xFF));

		UDPPacket udp =  new UDPPacket(packet, offs, len);
		udp.initInitialIPHeader();
		return udp;
	}

	public void updateHeader(int sourcePort, int destPort) {
		int[] hdrPacket = new int[2];
		hdrPacket[0] = (sourcePort << 16) + destPort;
		hdrPacket[1] = (len - ipHdrlen) << 16;  //len - IP Header length
		udpHeader.position(0);
		udpHeader.put(hdrPacket);
		hdrPacket[1] = hdrPacket[1] + calculateCheckSum(true);
		udpHeader.put(1, hdrPacket[1]);
	}

	public int checkCheckSum() {
		return calculateCheckSum(false);
	}

	private int calculateCheckSum(boolean internal) {
		int checkSum = 0;
		if (version == 4) {
			int saved = ipHeader.get(2); //preserve IP Header
			ipHeader.put(2, (17 << 16) + len - ipHdrlen);        // IP Pseudo Header (replace checksum by protocol (17 udp) and udp packet length for udp checksum calculation)
			checkSum = CheckSum.chkSum(data, offset + 8, len - 8);
			ipHeader.put(2, saved);  //restore the ip header
		} else if (version == 6) {
			int[] saved = new int[]{ipHeader.get(0), ipHeader.get(1)};
			ipHeader.put(0, len - ipHdrlen);   //UDP V6 Pseudo Header protocol UDP Length
			ipHeader.put(1, 17);                              // protocol 17
			checkSum = CheckSum.chkSum(data, offset, len);
			ipHeader.position(0);
			ipHeader.put(saved);
		} else
			throw new IllegalStateException("Illegal version:" + version);

		if (internal && checkSum == 0)
			checkSum = 0xffff;

		return checkSum;
	}

	public int getSourcePort() {
		return udpHeader.get(0) >>> 16;
	}

	public int getDestPort() {
		return udpHeader.get(0) & 0x0000FFFF;
	}

	public int getLength() {
		return udpHeader.get(1) >>> 16;
	}

	public int getIPPacketLength() {
		return super.getLength();
	}

	public int getHeaderLength() {
		return ipHdrlen + 8;
	}

	public int getOffset() {
		return super.getOffset() + ipHdrlen;
	}

	public int getIPPacketOffset() {
		return super.getOffset();
	}

}
