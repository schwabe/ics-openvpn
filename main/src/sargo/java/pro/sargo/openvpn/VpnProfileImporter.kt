package pro.sargo.openvpn

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.blinkt.openvpn.api.APIVpnProfile
import de.blinkt.openvpn.api.IOpenVPNAPIService
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Imports an OpenVPN profile into ics-openvpn through its AIDL API.
 *
 * The importer supports two config sources:
 *  - inline `.ovpn` configuration as a [String]
 *  - a `content://` URI read through [ContentResolver.openInputStream]
 *
 * No deprecated storage APIs are used.
 *
 * @param executor Executor used for background URI reads. Defaults to a single-thread executor.
 * @param mainHandler Handler used to deliver progress callbacks. Defaults to the main looper.
 */
class VpnProfileImporter(
    private val context: Context,
    private val executor: Executor = Executors.newSingleThreadExecutor(),
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {

    private val activeFutures = Collections.synchronizedList(mutableListOf<Future<*>>())

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
     * @param configTransformer Optional transform applied to the config before import.
     * @return [ImportResult.Success] with the imported profile, or [ImportResult.Error].
     */
    fun importFromString(
        service: IOpenVPNAPIService,
        name: String,
        config: String,
        configTransformer: (String) -> String = { it }
    ): ImportResult {
        val processedConfig = configTransformer(config)
        return try {
            val profile = service.addNewVPNProfile(name, false, processedConfig)
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
     *
     * @param configTransformer Optional transform applied to the config after it is read from the URI.
     */
    fun importFromUri(
        service: IOpenVPNAPIService,
        name: String,
        uri: Uri,
        listener: ImportProgressListener,
        configTransformer: (String) -> String = { it }
    ) {
        var futureRef: Future<*>? = null
        val task = Runnable {
            try {
                if (Thread.currentThread().isInterrupted) {
                    return@Runnable
                }
                post { listener.onImportStarted() }
                post { listener.onImportProgress("Reading configuration from $uri...") }

                val config = readConfigContent(uri)
                if (config == null) {
                    post {
                        listener.onImportCompleted(
                            ImportResult.Error("Could not read config from URI: $uri")
                        )
                    }
                    return@Runnable
                }

                post { listener.onImportProgress("Importing profile '$name'...") }
                val result = importFromString(service, name, config, configTransformer)
                post { listener.onImportCompleted(result) }
            } finally {
                futureRef?.let { activeFutures.remove(it) }
            }
        }

        futureRef = (executor as? ExecutorService)?.submit(task)
        futureRef?.let { activeFutures.add(it) } ?: executor.execute(task)
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

    /**
     * Cancel any URI imports that are still running. This should be called when the owning
     * component is destroyed (e.g. from an Activity's onDestroy) so callbacks cannot fire
     * after the UI has been torn down.
     */
    fun cancelActiveImports() {
        val copy = activeFutures.toList()
        activeFutures.clear()
        copy.forEach { it.cancel(true) }
    }

    /**
     * Shut down the internal executor. Should be called when the importer is no longer
     * needed (e.g. from the owning Activity's onDestroy) to avoid leaking threads.
     */
    fun shutdown() {
        cancelActiveImports()
        (executor as? ExecutorService)?.shutdown()
    }

    companion object {
        private const val TAG = "SargOVpnImporter"

        private fun InputStream.readText(): String {
            return bufferedReader().use { it.readText() }
        }
    }
}
