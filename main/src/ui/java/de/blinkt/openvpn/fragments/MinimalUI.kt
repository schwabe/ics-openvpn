/*
 * Copyright (c) 2012-2025 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.security.KeyChain
import android.security.KeyChainException
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.invalidateOptionsMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.blinkt.openvpn.LaunchVPN
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.VpnProfile.TYPE_KEYSTORE
import de.blinkt.openvpn.VpnProfile.TYPE_USERPASS_KEYSTORE
import de.blinkt.openvpn.activities.ConfigConverter
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.GlobalPreferences
import de.blinkt.openvpn.core.IOpenVPNServiceInternal
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.fragments.ImportRemoteConfig.Companion.newInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MinimalUI: Fragment(), VpnStatus.StateListener {
    private var mPermReceiver: ActivityResultLauncher<String>? = null
    private lateinit var mFileImportReceiver: ActivityResultLauncher<Intent?>
    private lateinit var profileManger: ProfileManager
    private var mService: IOpenVPNServiceInternal? = null
    private lateinit var vpnstatus: TextView
    private lateinit var vpntoggle: CompoundButton

    private lateinit var view: View
    private var mImportMenuActive = false

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

    private fun registerStartFileImportReceiver()
    {
        mFileImportReceiver = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult())
        {
                result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                val startImport = Intent(getActivity(), ConfigConverter::class.java)
                startImport.setAction(ConfigConverter.IMPORT_PROFILE)
                startImport.setData(uri)
                startActivity(startImport)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerPermissionReceiver()
        registerStartFileImportReceiver()
        setHasOptionsMenu(true)

        profileManger = ProfileManager.getInstance(requireContext());

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (GlobalPreferences.getAllowInitialImport() && ProfileManager.getAlwaysOnVPN(requireContext()) == null ) {
            mImportMenuActive = true
            menu.add(0, MENU_IMPORT_PROFILE, 0, R.string.menu_import)
                .setIcon(R.drawable.ic_menu_import)
                .setAlphabeticShortcut('i')
                .setTitleCondensed(getActivity()!!.getString(R.string.menu_import_short))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

            menu.add(0, MENU_IMPORT_AS, 0, R.string.import_from_as)
                .setIcon(R.drawable.ic_menu_import_download)
                .setAlphabeticShortcut('p')
                .setTitleCondensed("Import AS")
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
    }

    private fun startASProfileImport(): Boolean {
        val asImportFrag = newInstance(null)
        asImportFrag.show(getParentFragmentManager(), "dialog")
        invalidateOptionsMenu(activity)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.getItemId()

         if (itemId == MENU_IMPORT_PROFILE) {
            val intent =  Utils.getFilePickerIntent(getActivity()!!, Utils.FileType.OVPN_CONFIG)
             mFileImportReceiver.launch(intent)
             invalidateOptionsMenu(activity)

        } else if (itemId == MENU_IMPORT_AS) {
            return startASProfileImport()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        VpnStatus.addStateListener(this)

        val intent = Intent(requireActivity(), OpenVPNService::class.java)
        intent.action = OpenVPNService.START_SERVICE
        requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
        if (mImportMenuActive && ProfileManager.getAlwaysOnVPN(requireActivity()) != null )
            invalidateOptionsMenu(requireActivity())
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

    private suspend fun checkForKeychainPermission(v: View) {
        val keychainView = v.findViewById<View>(R.id.keychain_notification)

        val profile = ProfileManager.getAlwaysOnVPN(context)

        var permissionGranted = false
        withContext(Dispatchers.IO)
        {
            permissionGranted = (profile == null || !checkKeychainAccessIsMissing(profile))
        }


        keychainView.setVisibility(if (permissionGranted) View.GONE else View.VISIBLE)
        keychainView.setOnClickListener({

            try {
                KeyChain.choosePrivateKeyAlias(requireActivity(),
                    { alias ->
                        // Credential alias selected.  Remember the alias selection for future use.
                        profile.mAlias = alias
                        ProfileManager.saveProfile(context, profile)
                        viewLifecycleOwner.lifecycleScope.launch {
                            checkForKeychainPermission(v)
                        }
                    },
                    arrayOf("RSA", "EC"), null,
                    profile.mServerName,
                    -1,
                    profile.mAlias)
                // alias to preselect, null if unavailable
            } catch (anf: ActivityNotFoundException) {
                val ab = AlertDialog.Builder(activity)
                ab.setTitle(R.string.broken_image_cert_title)
                ab.setMessage(R.string.broken_image_cert)
                ab.setPositiveButton(android.R.string.ok, null)
                ab.show()
            }
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

        viewLifecycleOwner.lifecycleScope.launch {
        checkForKeychainPermission(view)
            }
        return view
    }

    fun checkKeychainAccessIsMissing(vp: VpnProfile): Boolean {
        if ((vp.mAuthenticationType != TYPE_USERPASS_KEYSTORE) && (vp.mAuthenticationType != TYPE_KEYSTORE)) {
            return false
        }

        if (TextUtils.isEmpty(vp.mAlias))
            return true
        val certs = vp.getExternalCertificates(context)
        if (certs == null)
            return true

        return false
    }

    fun checkVpnConfigured(): VpnProfile? {
        val alwaysOnVPN = ProfileManager.getAlwaysOnVPN(requireContext())
        if (alwaysOnVPN == null) {
            Toast.makeText(
                requireContext(),
                R.string.cannot_start_vpn_not_configured,
                Toast.LENGTH_SHORT
            ).show();
            return null
        }

        if (checkKeychainAccessIsMissing(alwaysOnVPN))
        {
            Toast.makeText(
                requireContext(),
                R.string.keychain_access,
                Toast.LENGTH_SHORT
            ).show()
            return null
        }
        return alwaysOnVPN
    }

    fun toggleSwitchPressed(view: CompoundButton) {
        viewLifecycleOwner.lifecycleScope.launch {toggleSwitchPressedReal(view) }
    }

    suspend fun toggleSwitchPressedReal(view: CompoundButton) {
        var alwaysOnVPN: VpnProfile? = null
        withContext(Dispatchers.IO) {
            alwaysOnVPN = checkVpnConfigured()
        }

        if (alwaysOnVPN == null)
        {
            view.setChecked(false)
            return
        }

        val intent = Intent(requireContext(), LaunchVPN::class.java)
        intent.putExtra(LaunchVPN.EXTRA_KEY, alwaysOnVPN.uuidString)
        intent.putExtra(OpenVPNService.EXTRA_START_REASON, "VPN started from homescreen.")
        intent.action = Intent.ACTION_MAIN
        startActivity(intent)
    }

    companion object {
        private val MENU_IMPORT_PROFILE = Menu.FIRST + 1
        private val MENU_IMPORT_AS = Menu.FIRST + 3
    }
}