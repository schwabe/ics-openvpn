/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

public class VPNLaunchHelper {
    private static final String MINIPIEVPN = "pie_openvpn";

    private static String writeMiniVPN(Context context) {
        String nativeAPI = NativeUtils.getNativeAPI();
        File libOvpnExec = new File(context.getApplicationInfo().nativeLibraryDir, "libovpnexec.so");
        /* Q does not allow executing binaries written in temp directory anymore */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            return libOvpnExec.getPath();

        String[] abis = Build.SUPPORTED_ABIS;

        if (!nativeAPI.equals(abis[0])) {
            VpnStatus.logWarning(R.string.abi_mismatch, Arrays.toString(abis), nativeAPI);
            abis = new String[]{nativeAPI};
        }

        for (String abi : abis) {

            File vpnExecutable = new File(context.getCacheDir(), "c_" + MINIPIEVPN + "." + abi);
            if ((vpnExecutable.exists() && vpnExecutable.canExecute()) || writeMiniVPNBinary(context, abi, vpnExecutable)) {
                return vpnExecutable.getPath();
            }
        }

        // Fixed: java.lang.RuntimeException: Cannot find any executable for this device's ABIs [armeabi-v7a, armeabi] on Samsung SM-A310F(Galaxy A3(2016))
        if (libOvpnExec.exists() && libOvpnExec.canExecute()) {
            return libOvpnExec.getAbsolutePath();
        }

        throw new RuntimeException("Cannot find any executable for this device's ABIs " + Arrays.toString(abis));
    }


    static String[] buildOpenvpnArgv(Context c) {
        Vector<String> args = new Vector<>();

        String binaryName = writeMiniVPN(c);
        // Add fixed paramenters
        //args.add("/data/data/de.blinkt.openvpn/lib/openvpn");

        args.add(binaryName);

        args.add("--config");
        args.add("stdin");

        return args.toArray(new String[0]);
    }

    private static boolean writeMiniVPNBinary(Context context, String abi, File mvpnout) {
        try {
            InputStream mvpn;

            try {
                mvpn = context.getAssets().open(MINIPIEVPN + "." + abi);
            } catch (IOException errabi) {
                VpnStatus.logInfo("Failed getting assets for architecture " + abi);
                return false;
            }


            FileOutputStream fout = new FileOutputStream(mvpnout);

            byte[] buf = new byte[4096];

            int lenread = mvpn.read(buf);
            while (lenread > 0) {
                fout.write(buf, 0, lenread);
                lenread = mvpn.read(buf);
            }
            fout.close();

            if (!mvpnout.setExecutable(true)) {
                VpnStatus.logError("Failed to make OpenVPN executable");
                return false;
            }


            return true;
        } catch (IOException e) {
            VpnStatus.logException(e);
            return false;
        }

    }


    public static void startOpenVpn(VpnProfile startprofile, Context context, String startReason, boolean replace_running_vpn) {
        Intent startVPN = startprofile.getStartServiceIntent(context, startReason, replace_running_vpn);
        if (startVPN != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                //noinspection NewApi
                context.startForegroundService(startVPN);
            else
                context.startService(startVPN);

        }
    }
}
