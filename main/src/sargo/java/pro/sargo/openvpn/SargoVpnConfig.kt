package pro.sargo.openvpn

/**
 * Parsed SargO remote VPN configuration.
 *
 * @property vpnName Display name for the imported profile (required).
 * @property vpnConfig Path/URI to the .ovpn file, or the inline config content (required).
 * @property connect Whether to start the VPN immediately after import.
 * @property alwaysOn Whether to set this profile as always-on VPN (null = leave unchanged).
 * @property remove Comma-separated list of existing profile names to remove.
 * @property removeAll Whether to remove all other VPN profiles before importing.
 */
data class SargoVpnConfig(
    val vpnName: String,
    val vpnConfig: String,
    val connect: Boolean = false,
    val alwaysOn: Boolean? = null,
    val remove: String = "",
    val removeAll: Boolean = false
) {
    companion object {
        /**
         * Build a [SargoVpnConfig] from raw SargO app preference strings.
         * Returns null when the configuration is incomplete or invalid.
         */
        fun fromPreferences(
            vpnName: String?,
            vpnConfig: String?,
            connect: String?,
            alwaysOn: String?,
            remove: String?,
            removeAll: String?
        ): SargoVpnConfig? {
            if (vpnName.isNullOrBlank() || vpnConfig.isNullOrBlank()) {
                return null
            }

            return SargoVpnConfig(
                vpnName = vpnName.trim(),
                vpnConfig = vpnConfig.trim(),
                connect = "1" == connect?.trim(),
                alwaysOn = when (alwaysOn?.trim()) {
                    "1" -> true
                    "0" -> false
                    else -> null
                },
                remove = remove?.trim() ?: "",
                removeAll = "1" == removeAll?.trim()
            )
        }
    }
}
