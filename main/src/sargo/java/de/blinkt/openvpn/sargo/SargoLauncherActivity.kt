package de.blinkt.openvpn.sargo

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import de.blinkt.openvpn.activities.MainActivity
import de.blinkt.openvpn.api.IOpenVPNAPIService
import pro.sargo.openvpn.SargoVpnController

/**
 * SargO-managed entry point for OpenVPN.
 *
 * This activity binds to the ics-openvpn AIDL service, applies the remote
 * SargO configuration, and then forwards the user to the standard MainActivity.
 */
class SargoLauncherActivity : Activity() {

    private var vpnService: IOpenVPNAPIService? = null
    private val controller by lazy { SargoVpnController(this) }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            vpnService = IOpenVPNAPIService.Stub.asInterface(service)
            Log.i(TAG, "Connected to OpenVPN API service")

            if (requestApiPermission()) {
                applyConfiguration()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            vpnService = null
            Log.i(TAG, "Disconnected from OpenVPN API service")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindOpenVpnService()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
            // Service was not bound
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_API_PERMISSION) {
            if (resultCode == RESULT_OK) {
                applyConfiguration()
            } else {
                Log.w(TAG, "OpenVPN API permission denied")
                openMainActivity()
            }
        }
    }

    private fun bindOpenVpnService() {
        val intent = Intent(IOpenVPNAPIService::class.java.name).apply {
            setPackage(packageName)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun requestApiPermission(): Boolean {
        val service = vpnService ?: return false
        return try {
            val intent = service.prepare(packageName)
            if (intent != null) {
                startActivityForResult(intent, REQUEST_API_PERMISSION)
                false
            } else {
                true
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to prepare OpenVPN API", e)
            false
        }
    }

    private fun applyConfiguration() {
        val service = vpnService
        if (service == null) {
            Log.w(TAG, "Cannot apply configuration: service not bound")
            openMainActivity()
            return
        }

        controller.applyConfiguration(service)
        openMainActivity()
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "SargOLauncher"
        private const val REQUEST_API_PERMISSION = 1001
    }
}
