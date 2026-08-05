package de.blinkt.openvpn.sargo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives SargO configuration update broadcasts and triggers the launcher activity,
 * which will re-apply the remote VPN configuration.
 */
class SargoConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIG_UPDATED) {
            return
        }

        Log.i(TAG, "Received SargO config update broadcast")

        val launchIntent = Intent(context, SargoLauncherActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(launchIntent)
    }

    companion object {
        private const val TAG = "SargOConfigReceiver"
        private const val ACTION_CONFIG_UPDATED = "pro.sargo.push.configUpdated"
    }
}
