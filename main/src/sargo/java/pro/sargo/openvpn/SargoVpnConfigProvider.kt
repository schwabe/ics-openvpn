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
        val vpnConfigContent = MDMService.Preferences.get(KEY_VPN_CONFIG_CONTENT, null)
        val connect = MDMService.Preferences.get(KEY_CONNECT, "0")
        val alwaysOn = MDMService.Preferences.get(KEY_ALWAYS_ON, null)
        val remove = MDMService.Preferences.get(KEY_REMOVE, "")
        val removeAll = MDMService.Preferences.get(KEY_REMOVE_ALL, "0")
        val disconnectOnConfigChange = MDMService.Preferences.get(KEY_DISCONNECT_ON_CONFIG_CHANGE, "0")
        val allowUserDisconnect = MDMService.Preferences.get(KEY_ALLOW_USER_DISCONNECT, null)
        val autoReconnect = MDMService.Preferences.get(KEY_AUTO_RECONNECT, "0")

        return SargoVpnConfig.fromPreferences(
            vpnName = vpnName,
            vpnConfig = vpnConfig,
            vpnConfigContent = vpnConfigContent,
            connect = connect,
            alwaysOn = alwaysOn,
            remove = remove,
            removeAll = removeAll,
            disconnectOnConfigChange = disconnectOnConfigChange,
            allowUserDisconnect = allowUserDisconnect,
            autoReconnect = autoReconnect
        )
    }

    companion object {
        const val KEY_VPN_NAME = "vpn_name"
        const val KEY_VPN_CONFIG = "vpn_config"
        const val KEY_VPN_CONFIG_CONTENT = "vpn_config_content"
        const val KEY_CONNECT = "connect"
        const val KEY_ALWAYS_ON = "always_on"
        const val KEY_REMOVE = "remove"
        const val KEY_REMOVE_ALL = "remove_all"
        const val KEY_DISCONNECT_ON_CONFIG_CHANGE = "disconnect_on_config_change"
        const val KEY_ALLOW_USER_DISCONNECT = "allow_user_disconnect"
        const val KEY_AUTO_RECONNECT = "auto_reconnect"
    }
}
