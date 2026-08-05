package pro.sargo.openvpn

import android.content.Context
import pro.sargo.MDMService

/**
 * Reads VPN configuration from SargO MDM app preferences.
 */
class SargoVpnConfigProvider(private val context: Context) {

    fun loadConfig(): SargoVpnConfig? {
        val vpnName = MDMService.Preferences.get(KEY_VPN_NAME, null)
        val vpnConfig = MDMService.Preferences.get(KEY_VPN_CONFIG, null)
        val connect = MDMService.Preferences.get(KEY_CONNECT, "0")
        val alwaysOn = MDMService.Preferences.get(KEY_ALWAYS_ON, null)
        val remove = MDMService.Preferences.get(KEY_REMOVE, "")
        val removeAll = MDMService.Preferences.get(KEY_REMOVE_ALL, "0")

        return SargoVpnConfig.fromPreferences(
            vpnName = vpnName,
            vpnConfig = vpnConfig,
            connect = connect,
            alwaysOn = alwaysOn,
            remove = remove,
            removeAll = removeAll
        )
    }

    companion object {
        const val KEY_VPN_NAME = "vpn_name"
        const val KEY_VPN_CONFIG = "vpn_config"
        const val KEY_CONNECT = "connect"
        const val KEY_ALWAYS_ON = "always_on"
        const val KEY_REMOVE = "remove"
        const val KEY_REMOVE_ALL = "remove_all"
    }
}
