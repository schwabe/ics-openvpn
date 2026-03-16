/*
 * Orchestrates the two-phase AWS Client VPN SAML authentication flow:
 *
 * Phase 1  — OpenVPN connects with username="N/A", password="ACS::35001".
 *             The server responds with AUTH_FAILED,CRV1::<flags>::<sid>::<url>.
 *             The management interface delivers this as:
 *               >PASSWORD:Verification Failed: 'Auth' ['CRV1:flags:sid:url']
 *             OpenVpnManagementThread calls AwsSamlAuthHandler.handleCrv1().
 *
 * Phase 2  — We start AwsSamlServer on :35001, open the SAML URL in the
 *             device's default browser, wait for the IdP to POST SAMLResponse
 *             back to our local server, then update the profile credentials to:
 *               username = "N/A"
 *               password = "CRV1::<sid>::<url-encoded-saml-response>"
 *             and call management.reconnect() so OpenVPN retries auth with
 *             the real SAML token.
 */
package de.blinkt.openvpn.aws

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import de.blinkt.openvpn.VpnProfile // kept for @Suppress JvmStatic signature
import de.blinkt.openvpn.core.OpenVPNManagement
import de.blinkt.openvpn.core.VpnStatus
import java.net.URLEncoder

private const val TAG = "AwsSamlAuthHandler"

/**
 * Stateless entry point called from Java.
 *
 * The actual work is done on a new background thread so the management
 * thread is never blocked.
 */
object AwsSamlAuthHandler {

    /**
     * Called by [de.blinkt.openvpn.core.OpenVpnManagementThread] when it
     * receives an AUTH_FAILED message whose args contain "CRV1:".
     *
     * @param context   The [OpenVPNService] (used to launch the browser).
     * @param management The management thread, used to trigger reconnect.
     * @param profile   The active [VpnProfile]; credentials are updated in place.
     * @param args      The raw args string from proccessPWFailed, e.g.
     *                  " ['CRV1:R,E:abcd1234:https://idp.example.com/saml?...']"
     */
    @JvmStatic
    fun handleCrv1(
        context: Context,
        management: OpenVPNManagement,
        @Suppress("UNUSED_PARAMETER") profile: VpnProfile,
        args: String
    ) {
        Thread({
            runCrv1Flow(context, management, args)
        }, "AwsSamlAuthFlow").apply {
            isDaemon = true
            start()
        }
    }

    // -------------------------------------------------------------------------

    private fun runCrv1Flow(
        context: Context,
        management: OpenVPNManagement,
        args: String
    ) {
        val parsed = parseCrv1(args)
        if (parsed == null) {
            Log.e(TAG, "Failed to parse CRV1 from args: $args")
            VpnStatus.logError("AWS SAML: Could not parse CRV1 challenge from server response")
            return
        }
        val (sid, samlUrl) = parsed

        Log.d(TAG, "CRV1 parsed — sid=$sid  url=$samlUrl")
        VpnStatus.logInfo("AWS SAML: Starting authentication — opening browser")

        val server = AwsSamlServer()

        server.start { result ->
            when (result) {
                is SamlResult.Success -> {
                    Log.d(TAG, "SAMLResponse received, sending credentials")
                    VpnStatus.logInfo("AWS SAML: Response received, authenticating…")

                    // URL-encode the decoded SAML response to embed it in the
                    // CRV1 credential string (matches the bash script behaviour).
                    val encodedSaml = URLEncoder.encode(result.samlResponse, "UTF-8")
                        .replace("+", "%20") // match Go's url.QueryEscape

                    // OpenVPN is already waiting for credentials on the open
                    // management socket (auth-retry interact). Send them
                    // directly — no SIGUSR1/restart needed.
                    management.sendSamlCredentials("N/A", "CRV1::${sid}::${encodedSaml}")
                }
                is SamlResult.Failure -> {
                    Log.e(TAG, "SAML server error: ${result.error}")
                    VpnStatus.logError("AWS SAML: Authentication failed — ${result.error}")
                }
            }
        }

        // Open the browser AFTER starting the server so we don't miss the POST.
        openSamlUrl(context, samlUrl)
    }

    /**
     * Parses the CRV1 data from [args].
     *
     * Input examples:
     *   " ['CRV1:R,E:abcd1234:https://idp.example.com/saml']"
     *   "['CRV1::E:abcd1234:https://idp.example.com/saml']"
     *
     * Returns Pair(sessionId, samlUrl) or null on parse failure.
     *
     * CRV1 format per OpenVPN spec: CRV1:<flags>:<client_cr>:<challenge_text>
     * where <challenge_text> is the SAML URL and may itself contain colons.
     * We split on ":" with limit=4 so everything from field [3] onward is
     * treated as the URL (preserving "https://...").
     */
    private fun parseCrv1(args: String): Pair<String, String>? {
        val start = args.indexOf("CRV1:")
        if (start < 0) return null

        // Strip everything up to and including the trailing "']" if present.
        val raw = args.substring(start).trimEnd('\'', ']', ' ')
        // raw = "CRV1:R,E:abcd1234:https://..."

        val parts = raw.split(":", limit = 4)
        if (parts.size < 4) return null
        // parts[0] = "CRV1"
        // parts[1] = flags (e.g. "R,E" or empty)
        // parts[2] = session id
        // parts[3] = saml URL (e.g. "https://...")

        val sid = parts[2].ifEmpty {
            Log.w(TAG, "CRV1 session ID is empty")
            return null
        }

        // AWS CRV1 has an extra field before the URL: CRV1:flags:sid:b'Ti9B':https://...
        // With limit=4, parts[3] = "b'Ti9B':https://..." so we find the actual URL.
        val urlStart = parts[3].indexOf("https://").takeIf { it >= 0 }
            ?: parts[3].indexOf("http://").takeIf { it >= 0 }
            ?: 0
        val url = parts[3].substring(urlStart).ifEmpty {
            Log.w(TAG, "CRV1 URL is empty")
            return null
        }

        return Pair(sid, url)
    }

    private fun openSamlUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        Log.d(TAG, "Opened browser for SAML URL")
    }
}
