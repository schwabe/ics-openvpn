package pro.sargo.openvpn

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
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

    private val mainHandler = Handler(Looper.getMainLooper())
    private val configProvider = SargoVpnConfigProvider(context)
    private val importer = VpnProfileImporter(context)
    private val alwaysOnManager = AlwaysOnVpnManager(context)

    /**
     * Apply the current SargO VPN configuration.
     *
     * @param service Bound IOpenVPNAPIService from ics-openvpn.
     */
    fun applyConfiguration(service: IOpenVPNAPIService) {
        val config = configProvider.loadConfig()
        if (config == null) {
            Log.v(TAG, "No valid SargO VPN configuration set")
            return
        }

        applyConfigurationInternal(service, config)
    }

    private fun applyConfigurationInternal(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig
    ) {
        // Remove requested profiles
        removeProfiles(service, config)

        // Check if profile already exists
        val existing = findProfileByName(service, config.vpnName)
        if (existing != null) {
            Log.v(TAG, "Profile '${config.vpnName}' already exists; skipping import")
            finalizeProfile(service, config, existing)
            return
        }

        // Import new profile
        importProfile(service, config) { imported ->
            if (imported == null) {
                Log.w(TAG, "Failed to import profile '${config.vpnName}'")
                return@importProfile
            }
            finalizeProfile(service, config, imported)
        }
    }

    private fun importProfile(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        callback: (APIVpnProfile?) -> Unit
    ) {
        when {
            config.vpnConfig.startsWith("content://") -> {
                importer.importFromUri(
                    service,
                    config.vpnName,
                    Uri.parse(config.vpnConfig),
                    object : VpnProfileImporter.ImportCallback {
                        override fun onProfileImported(profile: APIVpnProfile?) {
                            mainHandler.post { callback(profile) }
                        }
                    }
                )
            }
            config.vpnConfig.startsWith("/") -> {
                // Legacy file path; try to read via content resolver
                val uri = Uri.parse("file://${config.vpnConfig}")
                importer.importFromUri(
                    service,
                    config.vpnName,
                    uri,
                    object : VpnProfileImporter.ImportCallback {
                        override fun onProfileImported(profile: APIVpnProfile?) {
                            mainHandler.post { callback(profile) }
                        }
                    }
                )
            }
            else -> {
                // Treat as inline config content
                val profile = importer.importFromString(service, config.vpnName, config.vpnConfig)
                callback(profile)
            }
        }
    }

    private fun removeProfiles(service: IOpenVPNAPIService, config: SargoVpnConfig) {
        val profiles = try {
            service.profiles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list profiles", e)
            return
        }

        if (config.removeAll) {
            profiles.forEach { profile ->
                if (profile.name != config.vpnName) {
                    importer.removeProfile(service, profile.uuidString)
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

            profiles.forEach { profile ->
                if (namesToRemove.contains(profile.name)) {
                    importer.removeProfile(service, profile.uuidString)
                }
            }
        }
    }

    private fun findProfileByName(service: IOpenVPNAPIService, name: String): APIVpnProfile? {
        return try {
            service.profiles.find { it.name == name }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find profile by name", e)
            null
        }
    }

    private fun finalizeProfile(
        service: IOpenVPNAPIService,
        config: SargoVpnConfig,
        profile: APIVpnProfile
    ) {
        if (config.connect) {
            try {
                service.startProfile(profile.uuidString)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start profile '${profile.name}'", e)
            }
        }

        config.alwaysOn?.let { enabled ->
            val adminComponent = getAdminComponent()
            alwaysOnManager.setAlwaysOnVpn(adminComponent, enabled)
        }
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
