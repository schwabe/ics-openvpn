package de.blinkt.openvpn.core;

import android.os.Build;
import android.text.TextUtils;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.util.*;

public class NetworkSpace {


    static class ipAddress implements Comparable<ipAddress> {
        private BigInteger netAddress;
        public int networkMask;
        private boolean included;
        private boolean isV4;
        private BigInteger firstaddr;
        private BigInteger lastaddr;


        @Override
        public int compareTo(ipAddress another) {
            int comp = getFirstAddress().compareTo(another.getFirstAddress());
            if (comp != 0)
                return comp;

            // bigger mask means smaller address block
            if (networkMask > another.networkMask)
                return -1;
            else if (another.networkMask == networkMask)
                return 0;
            else
                return 1;
        }

        public ipAddress(CIDRIP ip, boolean include) {
            included = include;
            netAddress = BigInteger.valueOf(ip.getInt());
            networkMask = ip.len;
            isV4 = true;
        }

        public ipAddress(Inet6Address address, int mask, boolean include) {
            networkMask = mask;
            included = include;

            int s = 128;

            netAddress = BigInteger.ZERO;
            for (byte b : address.getAddress()) {
                s -= 16;
                netAddress = netAddress.add(BigInteger.valueOf(b).shiftLeft(s));
            }
        }

        public BigInteger getLastAddress() {
            if(lastaddr==null)
                lastaddr= getMaskedAddress(true);
            return lastaddr;
        }


        public BigInteger getFirstAddress() {
            if (firstaddr==null)
                firstaddr=getMaskedAddress(false);
            return firstaddr;
        }


        private BigInteger getMaskedAddress(boolean one) {
            BigInteger numAddress = netAddress;

            int numBits;
            if (isV4) {
                numBits = 32 - networkMask;
            } else {
                numBits = 128 - networkMask;
            }

            for (int i = 0; i < numBits; i++) {
                if (one)
                    numAddress = numAddress.setBit(i);
                else
                    numAddress = numAddress.clearBit(i);
            }
            return numAddress;
        }


        @Override
        public String toString() {
            //String in = included ? "+" : "-";
            if (isV4)
                return String.format(Locale.US,"%s/%d", getIPv4Address(), networkMask);
            else
                return String.format(Locale.US, "%s/%d", getIPv6Address(), networkMask);
        }

        ipAddress(BigInteger baseAddress, int mask, boolean included, boolean isV4) {
            this.netAddress = baseAddress;
            this.networkMask = mask;
            this.included = included;
            this.isV4 = isV4;
        }


        public ipAddress[] split() {
            ipAddress firsthalf = new ipAddress(getFirstAddress(), networkMask + 1, included, isV4);
            ipAddress secondhalf = new ipAddress(firsthalf.getLastAddress().add(BigInteger.ONE), networkMask + 1, included, isV4);
            assert secondhalf.getLastAddress().equals(getLastAddress());
            return new ipAddress[]{firsthalf, secondhalf};
        }

        String getIPv4Address() {
            assert (isV4);
            assert (netAddress.longValue() <= 0xffffffffl);
            assert (netAddress.longValue() >= 0);
            long ip = netAddress.longValue();
            return String.format(Locale.US, "%d.%d.%d.%d", (ip >> 24) % 256, (ip >> 16) % 256, (ip >> 8) % 256, ip % 256);
        }

        String getIPv6Address() {
            assert (!isV4);
            BigInteger r = netAddress;
            if (r.longValue() == 0)
                return "::";

            Vector<String> parts = new Vector<String>();
            while (r.compareTo(BigInteger.ZERO) == 1) {
                parts.add(0, String.format(Locale.US, "%x", r.mod(BigInteger.valueOf(256)).longValue()));
                r = r.shiftRight(16);
            }

            return TextUtils.join(":", parts);
        }

        public boolean containsNet(ipAddress network) {
            return getFirstAddress().compareTo(network.getFirstAddress()) != 1 &&
                    getLastAddress().compareTo(network.getLastAddress()) != -1;
        }
    }


    TreeSet<ipAddress> mIpAddresses = new TreeSet<ipAddress>();


    public Collection<ipAddress> getNetworks(boolean included) {
        Vector<ipAddress> ips = new Vector<ipAddress>();
        for (ipAddress ip : mIpAddresses) {
            if (ip.included == included)
                ips.add(ip);
        }
        return ips;
    }

    public void clear() {
        mIpAddresses.clear();
    }


    void addIP(CIDRIP cidrIp, boolean include) {

        mIpAddresses.add(new ipAddress(cidrIp, include));
    }

    void addIPv6(Inet6Address address, int mask, boolean included) {
        mIpAddresses.add(new ipAddress(address, mask, included));
    }

    TreeSet<ipAddress> generateIPList() {

        PriorityQueue<ipAddress> networks = new PriorityQueue<ipAddress>(mIpAddresses);

        TreeSet<ipAddress> ipsDone = new TreeSet<ipAddress>();

        ipAddress currentNet =  networks.poll();
        if (currentNet==null)
            return ipsDone;

        while (currentNet!=null) {
            // Check if it and the next of it are compatbile
            ipAddress nextNet = networks.poll();

            assert currentNet != null;
            if (nextNet== null || currentNet.getLastAddress().compareTo(nextNet.getFirstAddress()) == -1) {
                // Everything good, no overlapping nothing to do
                ipsDone.add(currentNet);

                currentNet = nextNet;
            } else {
                // This network is smaller or equal to the next but has the same base address
                if (currentNet.getFirstAddress().equals(nextNet.getFirstAddress()) && currentNet.networkMask >= nextNet.networkMask) {
                    if (currentNet.included == nextNet.included) {
                        // Included in the next next and same type
                        // Simply forget our current network
                        currentNet=nextNet;
                    } else {
                        // our currentnet is included in next and types differ. Need to split the next network
                        ipAddress[] newNets = nextNet.split();

                        // First add the second half to keep the order in networks
                        networks.add(newNets[1]);

                        if (newNets[0].getLastAddress().equals(currentNet.getLastAddress())) {
                            assert (newNets[0].networkMask == currentNet.networkMask);
                            // Don't add the lower half that would conflict with currentNet
                        } else {
                            networks.add(newNets[0]);
                        }
                        // Keep currentNet as is
                    }
                } else {
                    assert (currentNet.networkMask < nextNet.networkMask);
                    assert (nextNet.getFirstAddress().compareTo(currentNet.getFirstAddress()) == 1);
                    assert (currentNet.getLastAddress().compareTo(nextNet.getLastAddress()) != -1);

                    // This network is bigger than the next and last ip of current >= next

                    if (currentNet.included == nextNet.included) {
                        // Next network is in included in our network with the same type,
                        // simply ignore the next and move on
                    } else {
                        // We need to split our network
                        ipAddress[] newNets = currentNet.split();


                        if (newNets[1].networkMask == nextNet.networkMask) {
                            assert (newNets[1].getFirstAddress().equals(nextNet.getFirstAddress()));
                            assert (newNets[1].getLastAddress().equals(currentNet.getLastAddress()));
                            // Splitted second equal the next network, do not add it

                            networks.add(nextNet);
                        } else {
                            // Add the smaller network first
                            networks.add(newNets[1]);
                            networks.add(nextNet);
                        }
                        currentNet = newNets[0];

                    }
                }
            }

        }

        return ipsDone;
    }

    Collection<ipAddress> getPositiveIPList() {
        TreeSet<ipAddress> ipsSorted = generateIPList();

        Vector<ipAddress> ips = new Vector<ipAddress>();
        for (ipAddress ia : ipsSorted) {
            if (ia.included)
                ips.add(ia);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Include postive routes from the original set under < 4.4 since these might overrule the local
            // network but only if no smaller negative route exists
            for(ipAddress origIp: mIpAddresses){
                if (!origIp.included)
                    continue;

                // The netspace exists
                if(ipsSorted.contains(origIp))
                    continue;

                boolean skipIp=false;
                // If there is any smaller net that is excluded we may not add the positive route back
                for (ipAddress calculatedIp: ipsSorted) {
                    if(!calculatedIp.included && origIp.containsNet(calculatedIp)) {
                        skipIp=true;
                        break;
                    }
                }
                if (skipIp)
                    continue;

                // It is safe to include the IP
                ips.add(origIp);
            }

        }

        return ips;
    }

}
