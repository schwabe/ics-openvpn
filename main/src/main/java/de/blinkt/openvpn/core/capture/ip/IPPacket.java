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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class IPPacket {

	static short curID = (short) (Math.random() * Short.MAX_VALUE);
	static Object ID_SYNC = new Object();

	protected IntBuffer ipHeader;

	protected int version = 0;
	protected int len; // number of bytes of complete IP Packet (header plus data)!
	protected int ipHdrlen; //IP header length
	protected int offset;
	protected byte[] data;

	public IPPacket(byte[] packet, int offs, int len) {
		version = packet[offs] >> 4;
		data = packet;
		offset = offs;
		this.len = len;
		if (version == 4) {
			this.ipHeader = ByteBuffer.wrap(packet, offs, 20).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
			ipHdrlen = 20;
		} else if (version == 6) {
			this.ipHeader = ByteBuffer.wrap(packet, offs, 40).order(ByteOrder.BIG_ENDIAN).asIntBuffer();
			ipHdrlen = 40;
		} else
			throw new IllegalArgumentException("Invalid Version:" + version);
	}

	public static IPPacket createIPPacket(byte[] packet, int offs, int len, int version) {
		packet[offs] = ((byte) (version << 4 & 0xFF));
		return new IPPacket(packet, offs, len);
	}

	public static int[] ip2int(InetAddress ip) {
		byte[] b = ip.getAddress();
		if (b.length == 4)
			return new int[]{b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24};
		else
			return new int[]{
					b[3] & 0xFF | (b[2] & 0xFF) << 8 | (b[1] & 0xFF) << 16 | (b[0] & 0xFF) << 24,
					b[7] & 0xFF | (b[6] & 0xFF) << 8 | (b[5] & 0xFF) << 16 | (b[4] & 0xFF) << 24,
					b[11] & 0xFF | (b[10] & 0xFF) << 8 | (b[9] & 0xFF) << 16 | (b[8] & 0xFF) << 24,
					b[15] & 0xFF | (b[14] & 0xFF) << 8 | (b[13] & 0xFF) << 16 | (b[12] & 0xFF) << 24
			};
	}

	public static InetAddress int2ip(int[] ip) throws UnknownHostException {
		byte[] b;
		if (ip.length == 1)
			b = new byte[]{(byte) ((ip[0] >> 24) & 0xFF), (byte) ((ip[0] >> 16) & 0xFF), (byte) ((ip[0] >> 8) & 0xFF), (byte) (ip[0] & 0xFF)};
		else if (ip.length == 4) {
			b = new byte[]{
					(byte) ((ip[0] >> 24) & 0xFF), (byte) ((ip[0] >> 16) & 0xFF), (byte) ((ip[0] >> 8) & 0xFF), (byte) (ip[0] & 0xFF),
					(byte) ((ip[1] >> 24) & 0xFF), (byte) ((ip[1] >> 16) & 0xFF), (byte) ((ip[1] >> 8) & 0xFF), (byte) (ip[1] & 0xFF),
					(byte) ((ip[2] >> 24) & 0xFF), (byte) ((ip[2] >> 16) & 0xFF), (byte) ((ip[2] >> 8) & 0xFF), (byte) (ip[2] & 0xFF),
					(byte) ((ip[3] >> 24) & 0xFF), (byte) ((ip[3] >> 16) & 0xFF), (byte) ((ip[3] >> 8) & 0xFF), (byte) (ip[3] & 0xFF),
			};
		} else
			throw new IllegalArgumentException("Invalid array length:" + ip.length);

		return InetAddress.getByAddress(b);

	}

	private static int generateId() {
		synchronized (ID_SYNC) {
			curID++;
			return ((int) curID) << 16;
		}
	}


	private int calculateCheckSum() {
		if (version == 4)
			return CheckSum.chkSum(data, offset, 20);
		else
			return 0; // no checksum for IPV6
	}

	public int checkCheckSum() {
		return calculateCheckSum();
	}

	public void updateHeader(int TTL, int prot, int[] sourceIP, int[] destIP) {
		if (version == 4) {
			int[] hdrPacket = new int[5];
			hdrPacket[0] = 0x45000000 + len; // Version 4, IP header len (20 bytes /4 = 5), normal TOS (0) + complete pack length in bytes
			hdrPacket[1] = generateId(); // packet ID, fragmentation flags "0" and no fragmentation offset (0)
			hdrPacket[2] = (TTL << 24) + (prot << 16);
			hdrPacket[3] = sourceIP[0];
			hdrPacket[4] = destIP[0];
			ipHeader.position(0);
			ipHeader.put(hdrPacket);
			// add checksum
			hdrPacket[2] = hdrPacket[2] + calculateCheckSum();
			ipHeader.put(2, hdrPacket[2]);
		} else if (version == 6) {
			int[] hdrPacket = new int[2];
			hdrPacket[0] = version << 28; // Version = 6, Trafficclass = 0 (default), Flow Label = 0 (default);
			hdrPacket[1] = ((len - 40) << 16) + (prot << 8) + TTL;
			ipHeader.position(0);
			ipHeader.put(hdrPacket);
			ipHeader.put(sourceIP);
			ipHeader.put(destIP);
		} else
			throw new IllegalStateException("Illegal Version:" + version);
	}

	public int getVersion() {
		return version;
	}

	private int[] copyFromHeader(int pos, int count) {
		ipHeader.position(pos);
		int[] result = new int[count];
		ipHeader.get(result, 0, count);
		return result;
	}

	public int[] getSourceIP() {
		if (version == 4) {
			return copyFromHeader(3, 1);
		} else if (version == 6) {
			return copyFromHeader(2, 4);
		} else
			throw new IllegalStateException("Illegal Version:" + version);
	}

	public int[] getDestIP() {
		if (version == 4) {
			return copyFromHeader(4, 1);
		} else if (version == 6) {
			return copyFromHeader(6, 4);
		} else
			throw new IllegalStateException("Illegal Version:" + version);
	}

	public int getTTL() {
		if (version == 4)
			return ipHeader.get(2) >>> 24;
		else if (version == 6)
			return ipHeader.get(1) & 0xFF;
		else
			throw new IllegalStateException("Illegal Version:" + version);
	}

	public int getProt() {
		if (version == 4)
			return ipHeader.get(2) >>> 16 & 0x00FF;
		else if (version == 6)
			return ipHeader.get(1) >>> 8 & 0x00FF;
		else
			throw new IllegalStateException("Illegal Version:" + version);
	}

	public int getLength() {
		return ipHeader.get(0) & 0xFFFF;
	}

	public byte[] getData() {
		return data;
	}

	public int getOffset() {
		return offset;
	}

	public int getHeaderLength() {
		return ipHdrlen;
	}

}
