/*
 * Copyright (c) 2012-2026 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.core

import android.os.Build
import org.json.JSONException
import org.json.JSONObject
import kotlin.io.encoding.Base64

data class AccMessage(val protocol: String, val fragment: Boolean, val message: ByteArray)

public class AppCustomControl {

    public companion object {
        @JvmStatic
        public fun parseAccMessage(argument: String): AccMessage {
            // dpc1,0,eyJkcGNfcmVxdW...
            val arguments = argument.split(',', limit = 3)

            if (arguments.size < 3)
                throw IllegalArgumentException("Malformed ACC management message")

            val protocol: String = arguments[0]
            val b64message: String = arguments[2]

            val fragment = if (arguments[1] == "1") true else false

            val message = Base64.decode(b64message)

            return AccMessage(protocol, fragment, message)
        }
    }

}

class DPC1Protocol {
    var pendingMessage: String = ""

    /**
     * receives a dpc1 message and returns an appropriate response.
     *
     * This currently is a synchronous process as the current iteration
     * of the protocol does not require anything more advanced.
     *
     * @return a response message to be sent back to the client
     */
    fun processMessage(msg: AccMessage): AccMessage? {
        try {

            if (msg.protocol != "dpc1")
                throw IllegalArgumentException()

            pendingMessage += String(msg.message)

            if (msg.fragment)
                return null

            val data = pendingMessage
            pendingMessage = ""

            /* Fragment should be complete now */
            val command = JSONObject(data)
            val dpcRequest = command.getJSONObject("dpc_request")

            val version = dpcRequest.getString("ver")
            if (!version.matches("[0-9a-z.]*".toRegex())) {
                throw IllegalArgumentException("Invalid version number in dpc1 request")
            }

            var clientInfo = false
            if (dpcRequest.has("client_info")) {
                clientInfo = dpcRequest.getBoolean("client_info")
            }

            val antivirus = dpcRequest.has("antivirus")
            val disk_encryption = dpcRequest.has("disk_encryption")


            VpnStatus.logDebug("Received dpc1 request $version, clientinfo: $clientInfo, antivirus: $antivirus, disk_encryption: $disk_encryption")

            return createDpcResponse(reportClientInfo = clientInfo)
        } catch (je: JSONException) {
            VpnStatus.logError("Malformed dpc1 request: $je")
            return null
        }
    }

    fun createDpcResponse(reportClientInfo: Boolean): AccMessage {
        val response = JSONObject()
        response.put("dpc_response", JSONObject())
        val dpc_response = response.getJSONObject("dpc_response")

        if (reportClientInfo) {
            dpc_response.put("client_info", JSONObject())
            val clientInfo = dpc_response.getJSONObject("client_info")

            clientInfo.put("os", JSONObject())
            val os = clientInfo.getJSONObject("os")


            os.put("type", "Android")
            os.put("version", Build.VERSION.RELEASE)

            /* required by spec, Connexa gives internal server error if we actually send this */
            val arch =
                if (Build.SUPPORTED_ABIS.orEmpty().isEmpty()) "unknown" else Build.SUPPORTED_ABIS[0]

            os.put("extra", JSONObject())
            val extra = os.getJSONObject("extra")

            extra.put("arch", arch)
            extra.put("core_ver", Build.VERSION.SDK_INT)
        }

        dpc_response.put("ver", "1.0")

        val json = response.toString()

        val accResponse = AccMessage("dpc1", false, json.toByteArray())
        return accResponse
    }
}