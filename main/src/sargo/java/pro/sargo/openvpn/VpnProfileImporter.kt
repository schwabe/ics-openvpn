package pro.sargo.openvpn

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.blinkt.openvpn.api.APIVpnProfile
import de.blinkt.openvpn.api.IOpenVPNAPIService
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Imports an OpenVPN profile into ics-openvpn through its AIDL API.
 */
class VpnProfileImporter(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    interface ImportCallback {
        fun onProfileImported(profile: APIVpnProfile?)
    }

    /**
     * Import a profile from a raw .ovpn config string.
     *
     * @param service Bound IOpenVPNAPIService.
     * @param name Profile display name.
     * @param config Inline .ovpn configuration.
     * @return Imported profile, or null on failure.
     */
    fun importFromString(
        service: IOpenVPNAPIService,
        name: String,
        config: String
    ): APIVpnProfile? {
        return try {
            service.addNewVPNProfile(name, false, config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import VPN profile from string", e)
            null
        }
    }

    /**
     * Import a profile from a content:// URI asynchronously.
     *
     * @param service Bound IOpenVPNAPIService.
     * @param name Profile display name.
     * @param uri Content URI pointing to the .ovpn file.
     * @param callback Called on the main thread with the imported profile or null.
     */
    fun importFromUri(
        service: IOpenVPNAPIService,
        name: String,
        uri: Uri,
        callback: ImportCallback
    ) {
        executor.execute {
            val config = readConfigContent(uri)
            val profile = if (config != null) {
                importFromString(service, name, config)
            } else {
                Log.e(TAG, "Could not read config from URI: $uri")
                null
            }
            mainHandler.post {
                callback.onProfileImported(profile)
            }
        }
    }

    /**
     * Read the configuration content from a content:// URI.
     */
    private fun readConfigContent(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readText()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read config URI: $uri", e)
            null
        }
    }

    /**
     * Remove an existing profile by UUID.
     */
    fun removeProfile(service: IOpenVPNAPIService, uuid: String): Boolean {
        return try {
            service.removeProfile(uuid)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove profile $uuid", e)
            false
        }
    }

    companion object {
        private const val TAG = "SargOVpnImporter"

        private fun InputStream.readText(): String {
            return bufferedReader().use { it.readText() }
        }
    }
}
