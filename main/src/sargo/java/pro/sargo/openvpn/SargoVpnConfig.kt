package pro.sargo.openvpn

/**
 * Parsed SargO remote VPN configuration.
 *
 * @property vpnName Display name for the imported profile (required).
 * @property vpnConfig Path/URI to the .ovpn file, or the inline config content.
 * @property vpnConfigContent Optional inline .ovpn content. If provided, takes precedence over [vpnConfig].
 * @property connect Whether to start the VPN immediately after import.
 * @property alwaysOn Whether to set this profile as always-on VPN (null = leave unchanged).
 * @property remove Comma-separated list of existing profile names to remove.
 * @property removeAll Whether to remove all other VPN profiles before importing.
 * @property disconnectOnConfigChange Whether to disconnect the active VPN before applying a new configuration.
 * @property allowUserDisconnect Whether the user is allowed to manually disconnect the VPN (null = leave unchanged).
 * @property autoReconnect Whether to automatically reconnect after a network change or disconnect.
 */
data class SargoVpnConfig(
    val vpnName: String,
    val vpnConfig: String,
    val vpnConfigContent: String = "",
    val connect: Boolean = false,
    val alwaysOn: Boolean? = null,
    val remove: String = "",
    val removeAll: Boolean = false,
    val disconnectOnConfigChange: Boolean = false,
    val allowUserDisconnect: Boolean? = null,
    val autoReconnect: Boolean = false
) {
    companion object {
        /**
         * Build a [SargoVpnConfig] from raw SargO app preference strings.
         * Returns null when the configuration is incomplete or invalid.
         *
         * A configuration is considered valid when [vpnName] is non-blank and at least one of
         * [vpnConfig] or [vpnConfigContent] is non-blank.
         */
        fun fromPreferences(
            vpnName: String?,
            vpnConfig: String?,
            vpnConfigContent: String? = null,
            connect: String?,
            alwaysOn: String?,
            remove: String?,
            removeAll: String?,
            disconnectOnConfigChange: String? = null,
            allowUserDisconnect: String? = null,
            autoReconnect: String? = null
        ): SargoVpnConfig? {
            val trimmedName = vpnName?.trim()
            val trimmedConfig = vpnConfig?.trim() ?: ""
            val trimmedConfigContent = vpnConfigContent?.trim() ?: ""

            if (trimmedName.isNullOrBlank() || (trimmedConfig.isBlank() && trimmedConfigContent.isBlank())) {
                return null
            }

            return SargoVpnConfig(
                vpnName = trimmedName,
                vpnConfig = trimmedConfig,
                vpnConfigContent = trimmedConfigContent,
                connect = "1" == connect?.trim(),
                alwaysOn = when (alwaysOn?.trim()) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
                remove = remove?.trim() ?: "",
                removeAll = "1" == removeAll?.trim(),
                disconnectOnConfigChange = "1" == disconnectOnConfigChange?.trim(),
                allowUserDisconnect = when (allowUserDisconnect?.trim()) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
                autoReconnect = "1" == autoReconnect?.trim()
            )
        }
    }
}
