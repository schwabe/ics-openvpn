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
 *
 * The importer supports two config sources:
 *  - inline `.ovpn` configuration as a [String]
 *  - a `content://` URI read through [ContentResolver.openInputStream]
 *
 * No deprecated storage APIs are used.
 */
class VpnProfileImporter(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    sealed class ImportResult {
        data class Success(val profile: APIVpnProfile) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }

    interface ImportProgressListener {
        fun onImportStarted()
        fun onImportProgress(step: String)
        fun onImportCompleted(result: ImportResult)
    }

    /**
     * Import a profile from a raw .ovpn config string.
     *
     * @param service Bound IOpenVPNAPIService.
     * @param name Profile display name.
     * @param config Inline .ovpn configuration.
     * @return [ImportResult.Success] with the imported profile, or [ImportResult.Error].
     */
    fun importFromString(
        service: IOpenVPNAPIService,
        name: String,
        config: String
    ): ImportResult {
        return try {
            val profile = service.addNewVPNProfile(name, false, config)
                ?: return ImportResult.Error("OpenVPN API returned null profile")
            ImportResult.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import VPN profile from string", e)
            ImportResult.Error("Failed to import profile: ${e.message}")
        }
    }

    /**
     * Import a profile from a content:// URI asynchronously.
     *
     * Progress and result callbacks are delivered on the main thread.
     */
    fun importFromUri(
        service: IOpenVPNAPIService,
        name: String,
        uri: Uri,
        listener: ImportProgressListener
    ) {
        executor.execute {
            post { listener.onImportStarted() }
            post { listener.onImportProgress("Reading configuration from $uri...") }

            val config = readConfigContent(uri)
            if (config == null) {
                post {
                    listener.onImportCompleted(
                        ImportResult.Error("Could not read config from URI: $uri")
                    )
                }
                return@execute
            }

            post { listener.onImportProgress("Importing profile '$name'...") }
            val result = importFromString(service, name, config)
            post { listener.onImportCompleted(result) }
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

    private fun post(action: () -> Unit) {
        mainHandler.post(action)
    }

    companion object {
        private const val TAG = "SargOVpnImporter"

        private fun InputStream.readText(): String {
            return bufferedReader().use { it.readText() }
        }
    }
}
