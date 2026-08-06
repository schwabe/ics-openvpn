package pro.sargo.openvpn

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import de.blinkt.openvpn.api.APIVpnProfile
import de.blinkt.openvpn.api.IOpenVPNAPIService
import pro.sargo.SargoMDM

/**
 * Orchestrates the SargO remote VPN configuration flow:
 * 1. Read configuration from SargO MDM.
 * 2. Remove unwanted existing profiles.
 * 3. Import the new .ovpn profile.
 * 4. Optionally connect.
 * 5. Optionally set always-on VPN.
 */
class SargoVpnController(private val context: Context) {

    private val configProvider = SargoVpnConfigProvider(context)
    private val importer = VpnProfileImporter(context)
    private val alwaysOnManager = AlwaysOnVpnManager(context)
    private val configPostProcessor = OpenVpnConfigPostProcessor()

    interface ControllerListener {
        fun onStatus(message: String)
        fun onError(message: String)
        fun onCompleted()
    }

    /**
     * Apply the current SargO VPN configuration.
     *
     * @param service Bound IOpenVPNAPIService from ics-openvpn.
     * @param listener Optional listener for status/progress events.
     */
    fun applyConfiguration(
        service: IOpenVPNAPIService,
        listener: ControllerListener? = null
    ) {
        val config = configProvider.loadConfig()
        if (config == null) {
            Log.v(TAG, "No valid SargO VPN configuration set")
            listener?.onCompleted()
            return
        }

        listener?.onStatus("Applying SargO VPN configuration...")
        applyConfigurationInternal(service, config, listener)
    }

    private fun applyConfigurationInternal(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        listener: ControllerListener?
    ) {
        if (config.disconnectOnConfigChange) {
            listener?.onStatus("Disconnecting active VPN before applying new configuration...")
            try {
                service.disconnect()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to disconnect active VPN", e)
            }
        }

        // Remove requested profiles first
        removeProfiles(service, config, listener)

        // Check if the target profile already exists
        val existing = findProfileByName(service, config.vpnName)
        if (existing != null) {
            Log.v(TAG, "Profile '${config.vpnName}' already exists; skipping import")
            listener?.onStatus("Profile '${config.vpnName}' already exists")
            finalizeProfile(service, config, existing, listener)
            return
        }

        // Import the profile
        importProfile(service, config, listener)
    }

    private fun importProfile(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        listener: ControllerListener?
    ) {
        val inlineConfig = config.vpnConfigContent.ifBlank { config.vpnConfig }
        val configTransformer: (String) -> String = { rawConfig ->
            configPostProcessor.process(rawConfig, config)
        }

        when {
            inlineConfig.startsWith("content://") -> {
                importer.importFromUri(
                    service,
                    config.vpnName,
                    Uri.parse(inlineConfig),
                    object : VpnProfileImporter.ImportProgressListener {
                        override fun onImportStarted() {
                            listener?.onStatus("Importing profile '${config.vpnName}'...")
                        }

                        override fun onImportProgress(step: String) {
                            listener?.onStatus(step)
                        }

                        override fun onImportCompleted(result: VpnProfileImporter.ImportResult) {
                            when (result) {
                                is VpnProfileImporter.ImportResult.Success -> {
                                    finalizeProfile(service, config, result.profile, listener)
                                }
                                is VpnProfileImporter.ImportResult.Error -> {
                                    Log.w(TAG, result.message)
                                    listener?.onError(result.message)
                                    listener?.onCompleted()
                                }
                            }
                        }
                    },
                    configTransformer
                )
            }
            else -> {
                // Treat as inline config content
                listener?.onStatus("Importing profile '${config.vpnName}'...")
                val result = importer.importFromString(
                    service,
                    config.vpnName,
                    inlineConfig,
                    configTransformer
                )
                when (result) {
                    is VpnProfileImporter.ImportResult.Success -> {
                        finalizeProfile(service, config, result.profile, listener)
                    }
                    is VpnProfileImporter.ImportResult.Error -> {
                        Log.w(TAG, result.message)
                        listener?.onError(result.message)
                        listener?.onCompleted()
                    }
                }
            }
        }
    }

    private fun removeProfiles(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        listener: ControllerListener?
    ) {
        val profiles = try {
            service.getProfiles()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list profiles", e)
            listener?.onError("Failed to list profiles: ${e.message}")
            return
        }

        if (config.removeAll) {
            listener?.onStatus("Removing all other profiles...")
            profiles.forEach { profile ->
                if (profile.mName != config.vpnName) {
                    importer.removeProfile(service, profile.mUUID)
                }
            }
            return
        }

        if (config.remove.isNotBlank()) {
            val namesToRemove = config.remove
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()

            if (namesToRemove.isNotEmpty()) {
                listener?.onStatus("Removing selected profiles...")
            }
            profiles.forEach { profile ->
                if (namesToRemove.contains(profile.mName)) {
                    importer.removeProfile(service, profile.mUUID)
                }
            }
        }
    }

    private fun findProfileByName(service: IOpenVPNAPIService, name: String): APIVpnProfile? {
        return try {
            service.getProfiles().find { it.mName == name }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find profile by name", e)
            null
        }
    }

    private fun finalizeProfile(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        profile: APIVpnProfile,
        listener: ControllerListener?
    ) {
        if (config.connect) {
            listener?.onStatus("Connecting profile '${profile.mName}'...")
            try {
                service.startProfile(profile.mUUID)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start profile '${profile.mName}'", e)
                listener?.onError("Failed to start profile: ${e.message}")
            }
        }

        val effectiveAlwaysOn = config.alwaysOn ?: when (config.allowUserDisconnect) {
            false -> true
            else -> null
        }

        effectiveAlwaysOn?.let { enabled ->
            listener?.onStatus(
                if (enabled) "Enabling always-on VPN..." else "Disabling always-on VPN..."
            )
            val adminComponent = getAdminComponent()
            if (!alwaysOnManager.setAlwaysOnVpn(adminComponent, enabled)) {
                listener?.onError("Failed to set always-on VPN")
            }
        }

        listener?.onCompleted()
    }

    private fun getAdminComponent(): ComponentName? {
        return try {
            SargoMDM.getInstance().getAdminComponent(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get SargO admin component", e)
            null
        }
    }

    companion object {
        private const val TAG = "SargOVpnController"
    }
}
