package de.blinkt.openvpn;

class CIDRIP{
	String mIp;
	int len;
	public CIDRIP(String ip, String mask){
		mIp=ip;
		String[] ipt = mask.split("\\.");
		long netmask=0;

		netmask += Long.parseLong(ipt[0])<< 24;
		netmask += Integer.parseInt(ipt[1])<< 16;
		netmask += Integer.parseInt(ipt[2])<< 8;
		netmask += Integer.parseInt(ipt[3]);

		// Add 33. bit to ensure the loop terminates
		netmask += 1l << 32;

		int lenZeros = 0;
		while((netmask & 0x1) == 0) {
			lenZeros++;
			netmask = netmask >> 1;
		}
		// Check if rest of netmask is only 1s
		if(netmask != (0x1ffffffffl >> lenZeros)) {
			// Asume no CIDR, set /32
			len=32;
		} else {
			len =32 -lenZeros; 
		}

	}
	@Override
	public String toString() {
		return String.format("%s/%d",mIp,len);
	}

	public boolean normalise(){
		long ip=0;

		String[] ipt = mIp.split("\\.");

		ip += Long.parseLong(ipt[0])<< 24;
		ip += Integer.parseInt(ipt[1])<< 16;
		ip += Integer.parseInt(ipt[2])<< 8;
		ip += Integer.parseInt(ipt[3]);

		long newip = ip & (0xffffffffl << (32 -len));
		if (newip != ip){
			mIp = String.format("%d.%d.%d.%d", (newip & 0xff000000) >> 24,(newip & 0xff0000) >> 16, (newip & 0xff00) >> 8 ,newip & 0xff);
			return true;
		} else {
			return false;
		}
	}
}