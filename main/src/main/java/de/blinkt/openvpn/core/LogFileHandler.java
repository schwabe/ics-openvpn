/*
 * Copyright (c) 2012-2015 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;

import de.blinkt.openvpn.R;

/**
 * Created by arne on 23.01.16.
 */
class LogFileHandler extends Handler {
    static final int TRIM_LOG_FILE = 100;
    static final int FLUSH_TO_DISK = 101;
    static final int LOG_INIT = 102;
    public static final int LOG_MESSAGE = 103;
    private static FileOutputStream mLogFile;

    public static final String LOGFILE_NAME = "logcache.dat";


    public LogFileHandler(Looper looper) {
        super(looper);
    }


    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg.what == LOG_INIT) {
                if (mLogFile != null)
                    throw new RuntimeException("mLogFile not null");
                readLogCache((File) msg.obj);
                openLogFile((File) msg.obj);
            } else if (msg.what == LOG_MESSAGE && msg.obj instanceof VpnStatus.LogItem) {
                // Ignore log messages if not yet initialized
                if (mLogFile == null)
                    return;
                writeLogItemToDisk((VpnStatus.LogItem) msg.obj);
            } else if (msg.what == TRIM_LOG_FILE) {
                trimLogFile();
                for (VpnStatus.LogItem li : VpnStatus.getlogbuffer())
                    writeLogItemToDisk(li);
            } else if (msg.what == FLUSH_TO_DISK) {
                flushToDisk();
            }

        } catch (IOException e) {
            e.printStackTrace();
            VpnStatus.logError("Error during log cache: " + msg.what);
            VpnStatus.logException(e);
        }

    }

    private void flushToDisk() throws IOException {
        mLogFile.flush();
    }

    private static void trimLogFile() {
        try {
            mLogFile.flush();
            mLogFile.getChannel().truncate(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeLogItemToDisk(VpnStatus.LogItem li) throws IOException {
        Parcel p = Parcel.obtain();
        li.writeToParcel(p, 0);
        // We do not really care if the log cache breaks between Android upgrades,
        // write binary format to disc
        byte[] liBytes = p.marshall();

        byte[] lenBytes = ByteBuffer.allocate(4).putInt(liBytes.length).array();
        mLogFile.write(lenBytes);
        mLogFile.write(liBytes);
        p.recycle();
    }

    private void openLogFile (File cacheDir) throws FileNotFoundException {
        File logfile = new File(cacheDir, LOGFILE_NAME);
        mLogFile = new FileOutputStream(logfile);
    }

    private void readLogCache(File cacheDir) {
        File logfile = new File(cacheDir, LOGFILE_NAME);


        if (!logfile.exists() || !logfile.canRead())
            return;



        try {

            BufferedInputStream logFile = new BufferedInputStream(new FileInputStream(logfile));

            byte[] buf = new byte[8192];
            int read = logFile.read(buf, 0, 4);
            int itemsRead=0;

            while (read >= 4) {
                int len = ByteBuffer.wrap(buf, 0, 4).asIntBuffer().get();

                // Marshalled LogItem
                read = logFile.read(buf, 0, len);

                Parcel p = Parcel.obtain();
                p.unmarshall(buf, 0, read);
                p.setDataPosition(0);
                VpnStatus.LogItem li = VpnStatus.LogItem.CREATOR.createFromParcel(p);
                if (li.verify()) {
                    VpnStatus.newLogItem(li, true);
                } else {
                    VpnStatus.logError(String.format(Locale.getDefault(),
                            "Could not read log item from file: %d/%d: %s",
                            read, len, bytesToHex(buf, Math.max(read,80))));
                }
                p.recycle();

                //Next item
                read = logFile.read(buf, 0, 4);
                itemsRead++;
                if (itemsRead > 2*VpnStatus.MAXLOGENTRIES) {
                    VpnStatus.logError("Too many logentries read from cache, aborting.");
                    read = 0;
                }

            }
            VpnStatus.logDebug(R.string.reread_log, itemsRead);



        } catch (java.io.IOException | java.lang.RuntimeException e) {
            VpnStatus.logError("Reading cached logfile failed");
            VpnStatus.logException(e);
            e.printStackTrace();
            // ignore reading file error
        }
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes, int len) {
        len = Math.min(bytes.length, len);
        char[] hexChars = new char[len * 2];
        for ( int j = 0; j < len; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
