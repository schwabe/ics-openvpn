package de.blinkt.openvpn.core.capture;

import android.os.ParcelFileDescriptor;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import de.blinkt.openvpn.core.VpnStatus;
import de.blinkt.openvpn.core.capture.ip.IPPacket;
import de.blinkt.openvpn.core.capture.ip.UDPPacket;

public class StreamCapture {

    private static String VERSION="0.0.3";

    private static int FORWARD_DNS_PORT = 5300;
    private static StreamCapture INSTANCE = new StreamCapture();
    private static int BUFFER_SIZE = 20000;
    private static int MTU = 1500;
    private static int[] LOCAL_IP;
    private static int DNS=-1;
    private static boolean REDIRECT_DNS = false;

    public static String VIRTUAL_DNS = "10.10.10.10";

    private static String local_TO_remote = "local_TO_remote";
    private static String remote_TO_local = "remote_TO_local";
    private static TransferThread from_local;
    private static TransferThread from_remote;


    public static StreamCapture getInstance() {
        return INSTANCE;
    }

    public static void setMTU(int mtu){
        MTU = mtu;
    }

    public static void setLocalIP(String mIp)  {
        try {
            LOCAL_IP = IPPacket.ip2int(InetAddress.getByName(mIp));
        } catch (UnknownHostException e) {
           VpnStatus.logException(e);
        }
    }

    public static void setDNS(String dns) {
        if (dns == null) {
            INSTANCE.closeIgnoreException();
            DNS = -1;
            REDIRECT_DNS = false;
            return;
        }
        try {
            DNS = IPPacket.ip2int(InetAddress.getByName(dns))[0];
            REDIRECT_DNS = dns.equals(VIRTUAL_DNS);
            VpnStatus.logWarning("DNS:"+dns);
        } catch (UnknownHostException e) {
            VpnStatus.logException(e);
        }
    }


    private ParcelFileDescriptor remote = null;
    private ParcelFileDescriptor remote_stub = null;
    private ParcelFileDescriptor captured_fd = null;
    private FileInputStream remote_in = null;
    private FileOutputStream remote_out = null;
    private FileInputStream local_in = null;
    private FileOutputStream local_out = null;

    private boolean closed = true;


    private StreamCapture() {}  //private constructor


    private class TransferThread  implements Runnable {
        InputStream in;
        OutputStream out;
        String role;
        boolean fromLocal;
        boolean isClosed = false;

        public TransferThread(InputStream in, OutputStream out, String role){
            this.in = new BufferedInputStream(in,BUFFER_SIZE);
            this.out = out;
            this.role = role;
            fromLocal = role.equals(local_TO_remote);
            new Thread(this).start();
        }

        public void setClosed(){
            isClosed = true;
        }

        private void sleep(long millis) {
            Object obj = new Object();
            synchronized (obj) {
                try {
                    obj.wait(millis);
                } catch (Exception e) {
                }
            }
        }


        private int byteArrayToInt(int offs, byte[] b) {
            return b[offs+3] & 0xFF |
                   (b[offs+2] & 0xFF) << 8 |
                   (b[offs+1] & 0xFF) << 16 |
                   (b[offs] & 0xFF) << 24;
        }

        private int readInt(int offs, byte[] buf, InputStream in) throws IOException {
            int r = in.read(buf,offs, 4);
            if (r < 4)
                throw new IOException("Invalid packet data!");

            return byteArrayToInt(offs, buf);
        }

        private int readPacket(byte[] buf) throws IOException {

            int firstInt = readInt(0,buf,in);
            int offs = 4;

            int length = firstInt & 0xFFFF;
            int version = buf[0] >> 4;

            if (version != 4 && version != 6)
                throw new IOException("IP Version " + version + " not supported!");

            if (version == 6) {
                int nextInt = readInt(offs,buf,in);
                offs = 8;
                length = 40 + (nextInt >>> 16);
            }

            if (length > MTU)
                throw new IOException("Invalid IP header! MTU exceeded! MTU:" + MTU + ", Len:" + length + "!");

            while (offs < length) {
                int r = in.read(buf, offs, length - offs);
                offs = offs + r;
            }

            return offs;

            //return in.read(buf);
        }

        private int google_8_8_8_8 =  134744072;
        private int google_8_8_4_4 =  134743044;

        private boolean dropPacket(IPPacket ip) throws IOException {
            if (ip.getVersion() == 6) {
                VpnStatus.logWarning(role + ":Dropping IPV6 Package!\nIPlen:" + ip.getLength() + ", Proto:" + ip.getProt() + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ", Dest:" + IPPacket.int2ip(ip.getDestIP()));
                return true;
            }

            int dest = ip.getDestIP()[0];

            if (dest == google_8_8_8_8 || dest == google_8_8_4_4) {

                if (ip.getProt() == 17) {
                    UDPPacket udp = new UDPPacket(ip.getData(), ip.getOffset(), ip.getLength());
                    //drop google dns used silently by some ROMs
                    VpnStatus.logWarning(role + ":Dropping Google Package!\nIPlen:" + ip.getLength() + ", Proto:" + udp.getProt() + ", Source:" + IPPacket.int2ip(udp.getSourceIP()) + ":" + udp.getSourcePort() + ", Dest:" + IPPacket.int2ip(udp.getDestIP()) + ":" + udp.getDestPort());
                    return true;
                }
            }
            return false;
        }

        private void writeThrough(OutputStream out, byte[] buf, int offs, int len) throws IOException{
            synchronized(out){
                out.write(buf, offs, len);
                out.flush();
            }
        }


        @Override
        public void run() {

            VpnStatus.logWarning("Starting transfer:" + role + "!");

            int r = 0;
            while (r != -1 && !isClosed) {
                try {
                    byte[] buffer = new byte[MTU];

                    r = readPacket(buffer);

                    if (r == 0)
                        sleep(500);

                    else if (r != -1 && !isClosed && fromLocal) {

                        IPPacket ip = new IPPacket(buffer, 0, r);
                        int protocol = ip.getProt();
                        boolean drop = ip.getVersion()==6; //IP V6 not supported here
                        if (drop)
                            VpnStatus.logWarning(role + ":Dropping IPV6 Package!\nIPlen:" + ip.getLength() + ", Proto:" + ip.getProt() + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ", Dest:" + IPPacket.int2ip(ip.getDestIP()));

                        if (fromLocal && protocol == 17 && ip.getDestIP()[0] == DNS && (drop = dropPacket(ip)) == false){

                           UDPPacket udp = new UDPPacket(buffer, 0, r);

                           //DNS request or response => need to redirect

                           VpnStatus.logInfo("local->:" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + protocol + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ":" + udp.getSourcePort() + ", Dest:" + IPPacket.int2ip(ip.getDestIP()) + ":" + udp.getDestPort());

                           if (udp.getDestPort() == 53) {
                               // DNS request ==> redirect to local DNS Proxy on LOCAL_IP
                               udp.updateHeader(udp.getTTL(), 17, new int[]{DNS}, LOCAL_IP);
                               udp.updateHeader(udp.getSourcePort(), FORWARD_DNS_PORT);

                           } else {
                               //DNS response from local DNS Proxy on LOCAL_IP ==> redirect back to requestor
                               udp.updateHeader(udp.getTTL(), 17, new int[]{DNS}, LOCAL_IP);
                               udp.updateHeader(53, udp.getDestPort());
                           }

                           VpnStatus.logInfo("->local:" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + protocol + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ":" + udp.getSourcePort() + ", Dest:" + IPPacket.int2ip(ip.getDestIP()) + ":" + udp.getDestPort());

                           writeThrough(local_out, buffer, 0, r);

                        } else {
                            if (!drop) {
                                writeThrough(out, buffer, 0, r);
                                //VpnStatus.logInfo(role + ":" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + protocol + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ", Dest:" + IPPacket.int2ip(ip.getDestIP()));
                            }
                        }
                    }
                    if (r != -1 && !isClosed && !fromLocal) {
                        writeThrough(out, buffer, 0, r);
                    }

                } catch (IOException e) {
                    if (!isClosed) {
                        VpnStatus.logWarning(e.toString());
                        sleep(100);
                    }

                } catch (Exception e) {
                    if (!isClosed) {
                        VpnStatus.logException(e);
                        sleep(100);
                    }
                }
            }

            if (!isClosed)
                closeIgnoreException();

            VpnStatus.logWarning("Terminated transfer:" + role + "!");

        }
    }

    public synchronized void closeIgnoreException() {
        if (closed)
            return;

        from_local.setClosed();
        from_remote.setClosed();

        try {
            captured_fd.close();
        } catch (IOException e) {
        }
        try {
            local_in.close();
        } catch (IOException e) {
        }
        try {
            local_out.close();
        } catch (IOException e) {
        }
        try {
            remote.close();
        } catch (IOException e) {
        }
        try {
            remote_in.close();
        } catch (IOException e) {
        }
        try {
            remote_out.close();
        } catch (IOException e) {
        }
        try {
            remote_stub.close();
        } catch (IOException e) {
        }
        closed = true;
    }


    public ParcelFileDescriptor getCapturedParcelFileDescriptor(ParcelFileDescriptor pfd) throws IOException{

        if (!closed)
            closeIgnoreException();

        if (!REDIRECT_DNS)
            return pfd;

        VpnStatus.logWarning("StreamCapture Version:"+VERSION);

        closed = false;

        captured_fd = pfd;
        local_in = new FileInputStream(pfd.getFileDescriptor());
        local_out = new FileOutputStream(pfd.getFileDescriptor());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            ParcelFileDescriptor[] pair = ParcelFileDescriptor.createReliableSocketPair();
            remote_stub = pair[0];
            remote = pair[1];
            remote_out =new FileOutputStream(remote_stub.getFileDescriptor());
            remote_in = new FileInputStream(remote_stub.getFileDescriptor());
            from_local = new TransferThread(local_in, remote_out, local_TO_remote);
            from_remote = new TransferThread(remote_in, local_out, remote_TO_local);
            return remote;
        } else return null;

    }

}
