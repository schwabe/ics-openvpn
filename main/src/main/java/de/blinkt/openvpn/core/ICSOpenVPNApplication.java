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

/*
@ReportsCrashes(
        formKey = "",
        formUri = "http://reports.blinkt.de/report-icsopenvpn",
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin="report-icsopenvpn",
        formUriBasicAuthPassword="Tohd4neiF9Ai!!!!111eleven",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text
)
*/
public class ICSOpenVPNApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();

        if (BuildConfig.DEBUG) {
            //ACRA.init(this);
        }

        VpnStatus.initLogCache(getApplicationContext().getCacheDir());
    }
}
