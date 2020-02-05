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

    private static StreamCapture INSTANCE = new StreamCapture();
    private static int BUFFER_SIZE = 20000;
    private static int MTU = 1500;
    private static int[] LOCAL_IP;
    private static int DNS=-1;
    private static boolean REDIRECT_DNS = false;

    public static String VIRTUAL_DNS = "10.10.10.10";

    private static String fd_in_TO_local_out = "fd_in_TO_local_out";
    private static String local_in_TO_fd_out = "local_in_TO_fd_out";

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
            REDIRECT_DNS = false;
            return;
        }
        if (DNS !=-1)
            return;
        try {
            DNS = IPPacket.ip2int(InetAddress.getByName(dns))[0];
            REDIRECT_DNS = dns.equals(VIRTUAL_DNS);
            VpnStatus.logWarning("DNS:"+dns);
        } catch (UnknownHostException e) {
            VpnStatus.logException(e);
        }
    }


    private ParcelFileDescriptor remote = null;
    private ParcelFileDescriptor local = null;
    private ParcelFileDescriptor captured_fd = null;
    private FileInputStream local_in = null;
    private FileOutputStream local_out = null;
    private FileInputStream fd_in = null;
    private FileOutputStream fd_out = null;

    boolean closed = true;

    private StreamCapture() {}  //private constructor


    private class TransferThread  implements Runnable {
        InputStream in;
        OutputStream out;
        String role;
        boolean receiver;

        public TransferThread(InputStream in, OutputStream out, String role){
            this.in = new BufferedInputStream(in,BUFFER_SIZE);
            this.out = out;
            this.role = role;
            receiver = role.equals(fd_in_TO_local_out);
            new Thread(this).start();
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


        public int byteArrayToInt(byte[] b) {
            return b[3] & 0xFF |
                    (b[2] & 0xFF) << 8 |
                    (b[1] & 0xFF) << 16 |
                    (b[0] & 0xFF) << 24;
        }

        private int readPacket(byte[] buf) throws IOException {
           int r = in.read(buf,0,4);

            if (r == 0 || r == -1 )
                return r;

            if (r < 4)
                throw new IOException("Invalid packet data!");

            int length = byteArrayToInt(buf) & 0xFFFF;

            int offs = 4;

            while (offs < length) {
                r = in.read(buf,offs, length-offs);
                offs = offs+r;
            }

            return offs;

           //return in.read(buf);
        }

        @Override
        public void run() {

            VpnStatus.logWarning("Starting transfer:" + role + "!");

            try {
                int r = 0;
                while (r != -1) {
                    byte[] buffer = new byte[MTU];

                    r = readPacket(buffer);

                    if (r == 0)
                        sleep(500);

                    else if (r != -1) {

                        IPPacket ip = new IPPacket(buffer, 0, r);
                        UDPPacket udp = null;
                        if (receiver &&  ip.getProt() == 17 && ip.getDestIP()[0] == DNS)
                            udp = new UDPPacket(buffer, 0, r);

                        if (udp != null) {

                            //DNS request or response => need to redirect

                            VpnStatus.logWarning("fd_in ->:" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + ip.getProt() + ", Source:" + IPPacket.int2ip(ip.getSourceIP())+":"+udp.getSourcePort() + ", Dest:" + IPPacket.int2ip(ip.getDestIP())+":"+udp.getDestPort());

                            if (udp.getDestPort() == 53) {
                                // DNS request ==> redirect to local DNS Proxy on LOCAL_IP
                                udp.updateHeader(udp.getTTL(), 17, new int[]{DNS}, LOCAL_IP);
                                udp.updateHeader(udp.getSourcePort(), 5300);

                            } else {
                                //DNS response from local DNS Proxy on LOCAL_IP ==> redirect back to requestor
                                udp.updateHeader(udp.getTTL(), 17, new int[]{DNS}, LOCAL_IP);
                                udp.updateHeader(53, udp.getDestPort());
                            }

                            VpnStatus.logWarning("->fd_out:" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + ip.getProt() + ", Source:" + IPPacket.int2ip(ip.getSourceIP())+":"+udp.getSourcePort() + ", Dest:" + IPPacket.int2ip(ip.getDestIP())+":"+udp.getDestPort());
                            synchronized (fd_out){
                                fd_out.write(buffer, 0, r);
                                fd_out.flush();
                            }

                        } else {
                            synchronized (out) {
                                out.write(buffer, 0, r);
                                out.flush();
                            }
                            VpnStatus.logWarning(role + ":" + r + " Bytes, IPlen:" + ip.getLength() + ", Proto:" + ip.getProt() + ", Source:" + IPPacket.int2ip(ip.getSourceIP()) + ", Dest:" + IPPacket.int2ip(ip.getDestIP()));
                        }

                    }
                }

            } catch (Exception e) {
                VpnStatus.logWarning(e.toString());
            } finally {
                closeIgnoreException();
                VpnStatus.logWarning("Terminated transfer:" + role + "!");
            }

        }
    }

    public synchronized void closeIgnoreException() {
        if (closed)
            return;

        try {
            captured_fd.close();
        } catch (IOException e) {
        }
        try {
            fd_in.close();
        } catch (IOException e) {
        }
        try {
            fd_out.close();
        } catch (IOException e) {
        }
        try {
            remote.close();
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
            local.close();
        } catch (IOException e) {
        }
        closed = true;
        DNS=-1;
    }


    public ParcelFileDescriptor getCapturedParcelFileDescriptor(ParcelFileDescriptor pfd) throws IOException{

        if (!closed)
            closeIgnoreException();

        if (!REDIRECT_DNS)
            return pfd;

        closed = false;

        captured_fd = pfd;
        fd_in = new FileInputStream(pfd.getFileDescriptor());
        fd_out = new FileOutputStream(pfd.getFileDescriptor());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            ParcelFileDescriptor[] pair = ParcelFileDescriptor.createReliableSocketPair();
            local = pair[0];
            remote = pair[1];
            local_out=new FileOutputStream(local.getFileDescriptor());
            local_in = new FileInputStream(local.getFileDescriptor());
            new TransferThread(fd_in, local_out, fd_in_TO_local_out);
            new TransferThread(local_in, fd_out, local_in_TO_fd_out);
            return remote;
        } else return null;

    }

}
