package de.blinkt.openvpn.core;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.RestrictionEntry;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;

import de.blinkt.openvpn.R;

/**
 * Created by arne on 25.07.13.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GetRestrictionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        final PendingResult result = goAsync();

        new Thread() {
            @Override
            public void run() {
                final Bundle extras = new Bundle();

                ArrayList<RestrictionEntry> restrictionEntries = initRestrictions(context);

                extras.putParcelableArrayList(Intent.EXTRA_RESTRICTIONS_LIST, restrictionEntries);
                result.setResult(Activity.RESULT_OK,null,extras);
                result.finish();
            }
        }.run();
    }

    private ArrayList<RestrictionEntry> initRestrictions(Context context) {
        ArrayList<RestrictionEntry> restrictions = new ArrayList<RestrictionEntry>();
        RestrictionEntry allowChanges = new RestrictionEntry("allow_changes",false);
        allowChanges.setTitle(context.getString(R.string.allow_vpn_changes));
        restrictions.add(allowChanges);

        return restrictions;
    }
}
