/*
 * Copyright (c) 2012-2025 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus

class MinimalUI: Fragment(), VpnStatus.StateListener {
    private var mPermReceiver: ActivityResultLauncher<String>? = null
    private lateinit var profileManger: ProfileManager
    private var mService: IOpenVPNServiceInternal? = null
    private lateinit var vpnstatus: TextView
    private lateinit var vpntoggle: CompoundButton

    private lateinit var view: View

    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName,
            service: IBinder
        ) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mService = null
        }
    }

    private fun registerPermissionReceiver() {
        mPermReceiver = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { result: Boolean? ->
            checkForNotificationPermission(
                requireView()
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPermissionReceiver()

        profileManger = ProfileManager.getInstance(requireContext());
    }

    override fun onResume() {
        super.onResume()
        VpnStatus.addStateListener(this)

        val intent = Intent(requireActivity(), OpenVPNService::class.java)
        intent.action = OpenVPNService.START_SERVICE
        requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)

        /* Check the default VPN */
        val alwaysOnVPN: VpnProfile? = ProfileManager.getAlwaysOnVPN(requireContext())
        if (alwaysOnVPN == null) {
            vpnstatus.text = "Default VPN is not configured."
        }
    }

    override fun onPause() {
        super.onPause()
        VpnStatus.removeStateListener(this)

        requireActivity().unbindService(mConnection)
    }

    private fun checkForNotificationPermission(v: View) {
        val permissionView = v.findViewById<View>(R.id.notification_permission)
        val permissionGranted =
            requireActivity().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        permissionView.setVisibility(if (permissionGranted) View.GONE else View.VISIBLE)
        permissionView.setOnClickListener({ view: View? ->
            mPermReceiver?.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        })
    }

    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        Intent: Intent?
    ) {
        val cleanLogMessage = VpnStatus.getLastCleanLogMessage(activity, true)

        requireActivity().runOnUiThread {
            vpnstatus.setText(cleanLogMessage)
            val connected = level == ConnectionStatus.LEVEL_CONNECTED;
            vpntoggle.isChecked = connected
        }
    }

    override fun setConnectedVPN(uuid: String?) {
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.minimalui, container, false)
        vpntoggle = view.findViewById(R.id.vpntoggle)
        vpnstatus = view.findViewById(R.id.vpnstatus)

        vpntoggle.setOnClickListener { view ->
            toggleSwitchPressed(view as CompoundButton)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            checkForNotificationPermission(view)
        return view
    }

    fun toggleSwitchPressed(view: CompoundButton) {

        val alwaysOnVPN = ProfileManager.getAlwaysOnVPN(requireContext())
        if (alwaysOnVPN == null) {
            Toast.makeText(
                requireContext(),
                R.string.cannot_start_vpn_not_configured,
                Toast.LENGTH_SHORT
            ).show();
            view.setChecked(false)
            return
        }
        val intent = Intent(requireContext(), LaunchVPN::class.java)
        intent.putExtra(LaunchVPN.EXTRA_KEY, alwaysOnVPN.uuidString)
        intent.putExtra(OpenVPNService.EXTRA_START_REASON, "VPN started from homescreen.")
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
    }

}