/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core;
import android.app.Application;

/*
import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
*/

import de.blinkt.openvpn.BuildConfig;
import de.blinkt.openvpn.R;
import de.blinkt.openvpn.core.PRNGFixes;

public class ICSOpenVPNApplication extends Application {
    private StatusListener mStatus;

    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();

        mStatus = new StatusListener();
        mStatus.init(getApplicationContext());
    }
}
