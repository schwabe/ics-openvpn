package pro.sargo.openvpn

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Manages Android always-on VPN settings through the SargO device admin component.
 */
class AlwaysOnVpnManager(private val context: Context) {

    /**
     * Set or clear the always-on VPN package.
     *
     * @param adminComponent The SargO launcher device admin component.
     * @param enabled true to set this app as always-on VPN, false to clear it.
     * @return true if the operation succeeded.
     */
    fun setAlwaysOnVpn(adminComponent: ComponentName?, enabled: Boolean): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        if (adminComponent == null) {
            Log.w(TAG, "Cannot set always-on VPN: no admin component provided")
            return false
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false

        return try {
            dpm.setAlwaysOnVpnPackage(
                adminComponent,
                if (enabled) context.packageName else null,
                true /* lockdown */
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set always-on VPN", e)
            false
        }
    }

    companion object {
        private const val TAG = "SargOAlwaysOnVpn"
    }
}
