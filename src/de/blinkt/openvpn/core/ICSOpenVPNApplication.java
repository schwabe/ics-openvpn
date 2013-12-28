package de.blinkt.openvpn.core;

import android.app.Application;

/**
 * Created by arne on 28.12.13.
 */
public class ICSOpenVPNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
    }
}
