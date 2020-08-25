/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.PasswordDialogFragment.Companion.newInstance

class CredentialsPopup : AppCompatActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get the alarm ID from the intent extra data
        val intent = intent
        showPwDialog(intent)
    }

    private fun showPwDialog(intent: Intent) {
        val frag: DialogFragment? = newInstance(intent, true)
        if (frag == null) {
            finish()
            return
        }
        frag.show(supportFragmentManager, "dialog")
    }
}