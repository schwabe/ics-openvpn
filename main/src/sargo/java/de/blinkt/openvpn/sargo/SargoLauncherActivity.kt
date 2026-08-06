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
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import de.blinkt.openvpn.R
import de.blinkt.openvpn.activities.MainActivity
import de.blinkt.openvpn.api.IOpenVPNAPIService
import pro.sargo.SargoMDM
import pro.sargo.openvpn.SargoVpnController

/**
 * SargO-managed entry point for OpenVPN.
 *
 * This activity:
 * 1. Connects to the SargO MDM service and waits for it to be ready.
 * 2. Binds to the ics-openvpn AIDL service and requests API permission.
 * 3. Applies the remote SargO VPN configuration.
 * 4. Forwards the user to the standard MainActivity.
 */
class SargoLauncherActivity : Activity() {

    private var vpnService: IOpenVPNAPIService? = null
    private val controller by lazy { SargoVpnController(this) }

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    private var isSargoConnected = false
    private var isOpenVpnBound = false

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

    private val mdmEventHandler = object : SargoMDM.EventHandler {
        override fun onSargoMDMConnected() {
            Log.i(TAG, "Connected to SargO MDM")
            isSargoConnected = true
            runOnUiThread { bindOpenVpnService() }
        }

        override fun onSargoMDMDisconnected() {
            Log.w(TAG, "Disconnected from SargO MDM")
            isSargoConnected = false
        }

        override fun onSargoMDMConfigChanged() {
            Log.i(TAG, "SargO config changed")
            vpnService?.let { applyConfiguration() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sargo_launcher)

        progressBar = findViewById(R.id.sargo_progress)
        statusText = findViewById(R.id.sargo_status_text)

        setStatus("Connecting to SargO MDM...")
        connectSargo()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isSargoConnected) {
            SargoMDM.getInstance().disconnect(this)
            isSargoConnected = false
        }
        if (isOpenVpnBound) {
            try {
                unbindService(serviceConnection)
            } catch (e: IllegalArgumentException) {
                // Service was not bound
            }
            isOpenVpnBound = false
        }
        controller.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_API_PERMISSION) {
            if (resultCode == RESULT_OK) {
                applyConfiguration()
            } else {
                Log.w(TAG, "OpenVPN API permission denied")
                setStatus("VPN API permission denied")
                openMainActivity()
            }
        }
    }

    private fun connectSargo() {
        val connected = try {
            SargoMDM.getInstance().connect(this, mdmEventHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to SargO MDM", e)
            false
        }

        if (!connected) {
            Log.w(TAG, "SargO MDM not available; opening standard UI")
            setStatus("SargO MDM not available")
            openMainActivity()
        }
    }

    private fun bindOpenVpnService() {
        val intent = Intent(IOpenVPNAPIService::class.java.name).apply {
            setPackage(packageName)
        }
        isOpenVpnBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        if (!isOpenVpnBound) {
            Log.w(TAG, "Failed to bind OpenVPN API service")
            setStatus("Failed to bind OpenVPN service")
            openMainActivity()
        }
    }

    private fun requestApiPermission(): Boolean {
        val service = vpnService ?: return false
        return try {
            val intent = service.prepare(packageName)
            if (intent != null) {
                // Make the intent explicit to avoid StrictMode UnsafeIntentLaunchViolation
                // on Android 12+ when the returned intent only carries a component.
                intent.setPackage(packageName)
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

        controller.applyConfiguration(service, object : SargoVpnController.ControllerListener {
            override fun onStatus(message: String) {
                setStatus(message)
            }

            override fun onError(message: String) {
                setStatus(message)
            }

            override fun onCompleted() {
                openMainActivity()
            }
        })
    }

    private fun setStatus(message: String) {
        statusText.text = message
        statusText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        Log.i(TAG, "Status: $message")
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
