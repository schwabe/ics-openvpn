/*
 * Local HTTP server that listens on 127.0.0.1:35001 for the SAML POST
 * callback from the AWS Client VPN authentication flow.
 *
 * AWS sets the ACS (Assertion Consumer Service) URL to http://127.0.0.1:35001
 * when we initiate auth with password "ACS::35001". After the user completes
 * IdP login, the browser is redirected back here with a SAMLResponse POST.
 */
package de.blinkt.openvpn.aws

import android.util.Log
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URLDecoder

private const val TAG = "AwsSamlServer"
private const val SAML_PORT = 35001
private const val ACCEPT_TIMEOUT_MS = 5 * 60 * 1000 // 5 minutes

/** Result of a SAML callback: either the response or an error. */
sealed class SamlResult {
    data class Success(val samlResponse: String) : SamlResult()
    data class Failure(val error: String) : SamlResult()
}

/**
 * Single-use HTTP server that captures one SAML POST then stops.
 *
 * Usage:
 *   val server = AwsSamlServer()
 *   server.start { result -> ... }
 *   // later, if needed:
 *   server.stop()
 */
class AwsSamlServer {

    @Volatile private var serverSocket: ServerSocket? = null

    /**
     * Starts the server on a daemon thread. [onResult] is called exactly once
     * on that thread with either Success or Failure.
     */
    fun start(onResult: (SamlResult) -> Unit) {
        Thread({
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.soTimeout = ACCEPT_TIMEOUT_MS
                ss.bind(InetSocketAddress("127.0.0.1", SAML_PORT))
                serverSocket = ss

                Log.d(TAG, "Listening on 127.0.0.1:$SAML_PORT for SAML POST")

                ss.accept().use { client ->
                    val saml = readSamlResponse(client.getInputStream())
                    sendOkResponse(client.getOutputStream())
                    if (saml != null) {
                        Log.d(TAG, "Received SAMLResponse (${saml.length} chars)")
                        onResult(SamlResult.Success(saml))
                    } else {
                        onResult(SamlResult.Failure("SAMLResponse field missing from POST body"))
                    }
                }
            } catch (e: Exception) {
                if (serverSocket?.isClosed == false) {
                    Log.e(TAG, "Server error", e)
                    onResult(SamlResult.Failure(e.message ?: "Unknown server error"))
                }
                // else: stop() was called intentionally, ignore
            } finally {
                serverSocket?.closeQuietly()
            }
        }, "AwsSamlServer").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        serverSocket?.closeQuietly()
    }

    // -------------------------------------------------------------------------

    /**
     * Reads an HTTP POST request from [inputStream] and returns the
     * URL-decoded value of the "SAMLResponse" form field, or null.
     */
    private fun readSamlResponse(inputStream: java.io.InputStream): String? {
        // Read until the blank line separating headers from body (\r\n\r\n).
        val headerBuf = ByteArrayOutputStream()
        val window = ByteArray(4)
        var windowFilled = 0

        while (true) {
            val b = inputStream.read()
            if (b < 0) break
            headerBuf.write(b)
            // Slide the 4-byte window
            if (windowFilled < 4) {
                window[windowFilled++] = b.toByte()
            } else {
                window[0] = window[1]; window[1] = window[2]
                window[2] = window[3]; window[3] = b.toByte()
            }
            // Detect \r\n\r\n
            if (windowFilled == 4 &&
                window[0] == '\r'.code.toByte() && window[1] == '\n'.code.toByte() &&
                window[2] == '\r'.code.toByte() && window[3] == '\n'.code.toByte()
            ) break
        }

        val headers = headerBuf.toString(Charsets.UTF_8.name())
        val contentLength = Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE)
            .find(headers)?.groupValues?.get(1)?.toIntOrNull() ?: return null

        if (contentLength <= 0) return null

        // Read exactly contentLength bytes for the body.
        val body = ByteArray(contentLength)
        var offset = 0
        while (offset < contentLength) {
            val read = inputStream.read(body, offset, contentLength - offset)
            if (read < 0) break
            offset += read
        }

        return parseFormParam(String(body, Charsets.UTF_8), "SAMLResponse")
    }

    /**
     * Parses [param] out of a URL-encoded form body like
     * "key1=val1&SAMLResponse=abc%2B...&key2=val2".
     */
    private fun parseFormParam(body: String, param: String): String? =
        body.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it.size == 2 && it[0] == param }
            ?.get(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }

    private fun sendOkResponse(out: java.io.OutputStream) {
        val html = "<html><body>Authentication successful. You may close this tab.</body></html>"
        val response = buildString {
            append("HTTP/1.1 200 OK\r\n")
            append("Content-Type: text/html; charset=UTF-8\r\n")
            append("Content-Length: ${html.length}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
            append(html)
        }
        out.write(response.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    private fun ServerSocket.closeQuietly() = try { close() } catch (_: Exception) {}
}
