package de.blinkt.openvpn.core;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.util.Arrays;

public class TestLogFileHandler {

    byte[] testUnescaped = new byte[] {0x00, 0x55, -27, 0x00, 0x56, 0x10, -128, 0x55, 0x54};
    byte[] expectedEscaped = new byte[] {0x55, 0x00, 0x00, 0x00, 0x09, 0x00, 0x56, 0x00, -27, 0x00, 0x56, 0x01, 0x10, -128, 0x56, 0x00, 0x54};
    private TestingLogFileHandler lfh;


    @Before
    public void setup()
    {
        lfh = new TestingLogFileHandler();
    }

    @Test
    public void testWriteByteArray() throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        lfh.setLogFile(byteArrayOutputStream);

        lfh.writeEscapedBytes(testUnescaped);

        byte[] result = byteArrayOutputStream.toByteArray();
        Assert.assertTrue(Arrays.equals(expectedEscaped, result));
    }

    @Test
    public void readByteArray() throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(expectedEscaped);

        lfh.readCacheContents(in);

        Assert.assertTrue(Arrays.equals(testUnescaped, lfh.mRestoredByteArray));

    }

    @SuppressLint("HandlerLeak")
    static class TestingLogFileHandler extends LogFileHandler {

        public byte[] mRestoredByteArray;

        public TestingLogFileHandler() {
            super(null);
        }

        public void setLogFile(OutputStream out) {
            mLogFile = out;
        }

        @Override
        public void readCacheContents(InputStream in) throws IOException {
            super.readCacheContents(in);
        }

        @Override
        protected void restoreLogItem(byte[] buf, int len) {
            mRestoredByteArray = Arrays.copyOf(buf, len);
        }
    }
}