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

public class CheckSum {

	public static int chkSum(byte[] buf, int off, int cnt) {

		int sum = 0;

		for (int i = 0; i < cnt; i = i + 2) {
			int val = ((buf[off + i] & 0xFF) << 8);
			if (i + 1 < cnt)
				val = val + (buf[off + i + 1] & 0xFF);

			sum = sum + val;
		}

		while ((sum >> 16) != 0)
			sum = (sum & 0xffff) + (sum >> 16);

		sum = (~sum) & 0xffff;

		return sum;
	}
}
