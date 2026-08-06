package pro.sargo.openvpn

/**
 * Applies SargO-specific overrides to a raw OpenVPN configuration before it is imported.
 *
 * The post-processor does not parse the full OpenVPN grammar; it only injects or replaces
 * a small set of directives that the SargO MDM layer controls:
 *  - inline username/password via `<auth-user-pass>`
 *  - log verbosity via `verb`
 *  - auto-reconnect hints via `persist-tun` / `persist-key`
 */
class OpenVpnConfigPostProcessor {

    /**
     * Apply the SargO overrides from [config] to a raw OpenVPN configuration string.
     *
     * @param config The raw .ovpn configuration.
     * @param sargoConfig The parsed SargO configuration holding the overrides.
     * @return The modified configuration ready for import.
     */
    fun process(config: String, sargoConfig: SargoVpnConfig): String {
        val lines = config.lines().toMutableList()

        removeExistingInlineAuthUserPass(lines)
        injectCredentials(lines, sargoConfig)
        injectLogLevel(lines, sargoConfig)
        injectReconnectHints(lines, sargoConfig)

        return lines.joinToString("\n").trimEnd().plus("\n")
    }

    private fun injectCredentials(lines: MutableList<String>, sargoConfig: SargoVpnConfig) {
        val username = sargoConfig.vpnUsername?.takeIf { it.isNotBlank() } ?: return
        val password = sargoConfig.vpnPassword ?: ""

        // Remove any existing auth-user-pass directive so the inline block is authoritative.
        lines.removeAll { it.isAuthUserPassDirective() }

        lines.add("")
        lines.add("<auth-user-pass>")
        lines.add(username)
        lines.add(password)
        lines.add("</auth-user-pass>")
    }

    private fun injectLogLevel(lines: MutableList<String>, sargoConfig: SargoVpnConfig) {
        val level = sargoConfig.logLevel ?: return

        // Remove any existing verb directive so the injected value wins.
        lines.removeAll { it.isVerbDirective() }

        lines.add("")
        lines.add("verb $level")
    }

    private fun injectReconnectHints(lines: MutableList<String>, sargoConfig: SargoVpnConfig) {
        if (!sargoConfig.autoReconnect) return

        if (lines.none { it.isStandaloneDirective("persist-tun") }) {
            lines.add("")
            lines.add("persist-tun")
        }
        if (lines.none { it.isStandaloneDirective("persist-key") }) {
            lines.add("")
            lines.add("persist-key")
        }
    }

    /**
     * Remove any existing inline `<auth-user-pass>..</auth-user-pass>` block so the
     * credentials injected by [injectCredentials] are unambiguous.
     */
    private fun removeExistingInlineAuthUserPass(lines: MutableList<String>) {
        var inside = false
        lines.removeAll { line ->
            val trimmed = line.trim()
            when {
                trimmed.equals("<auth-user-pass>", ignoreCase = true) -> {
                    inside = true
                    true
                }
                trimmed.equals("</auth-user-pass>", ignoreCase = true) -> {
                    inside = false
                    true
                }
                else -> inside
            }
        }
    }

    private fun String.isAuthUserPassDirective(): Boolean {
        val trimmed = trim()
        if (!trimmed.startsWith("auth-user-pass", ignoreCase = true)) return false
        // Do not match auth-user-pass-verify or similar directives.
        val after = trimmed.substring("auth-user-pass".length)
        return after.isEmpty() || after.first().isWhitespace()
    }

    private fun String.isVerbDirective(): Boolean {
        val parts = trim().split(Regex("\\s+"))
        return parts.size == 2 && parts[0].equals("verb", ignoreCase = true) && parts[1].toIntOrNull() != null
    }

    private fun String.isStandaloneDirective(name: String): Boolean {
        val parts = trim().split(Regex("\\s+"))
        return parts.size == 1 && parts[0].equals(name, ignoreCase = true)
    }
}
