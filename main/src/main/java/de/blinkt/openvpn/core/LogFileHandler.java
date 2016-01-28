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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by arne on 23.01.16.
 */
class LogFileHandler extends Handler {
    static final int TRIM_LOG_FILE = 100;
    static final int FLUSH_TO_DISK = 101;
    static final int LOG_INIT = 102;
    public static final int LOG_MESSAGE = 103;
    private static FileOutputStream mLogFile;
    private static BufferedOutputStream mBufLogfile;

    public static final String LOGFILE_NAME = "logcache.dat";


    public LogFileHandler(Looper looper) {
        super(looper);
    }


    @Override
    public void handleMessage(Message msg) {
        try {
            if (msg.what == LOG_INIT) {
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
            mBufLogfile.flush();
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

        mLogFile.write(liBytes.length & 0xff);
        mLogFile.write(liBytes.length >> 8);
        mLogFile.write(liBytes);
        p.recycle();
    }

    private void openLogFile (File cacheDir) throws FileNotFoundException {
        File logfile = new File(cacheDir, LOGFILE_NAME);
        mLogFile = new FileOutputStream(logfile);
        mBufLogfile = new BufferedOutputStream(mLogFile);
    }

    private void readLogCache(File cacheDir) {
        File logfile = new File(cacheDir, LOGFILE_NAME);

        if (!logfile.exists() || !logfile.canRead())
            return;

        VpnStatus.logDebug("Reread log items from cache file");

        try {
            BufferedInputStream logFile = new BufferedInputStream(new FileInputStream(logfile));

            byte[] buf = new byte[8192];
            int read = logFile.read(buf, 0, 2);

            while (read > 0) {
                // Marshalled LogItem
                int len = (0xff & buf[0]) | buf[1] << 8;

                read = logFile.read(buf, 0, len);

                Parcel p = Parcel.obtain();
                p.unmarshall(buf, 0, read);
                p.setDataPosition(0);
                VpnStatus.LogItem li = VpnStatus.LogItem.CREATOR.createFromParcel(p);
                VpnStatus.newLogItem(li, true);
                p.recycle();

                //Next item
                read = logFile.read(buf, 0, 2);
            }

        } catch (java.io.IOException | java.lang.RuntimeException e) {
            VpnStatus.logError("Reading cached logfile failed");
            VpnStatus.logException(e);
            e.printStackTrace();
            // ignore reading file error
        }
    }

}
