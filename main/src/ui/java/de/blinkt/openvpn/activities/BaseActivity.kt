/*
 * Copyright (c) 2012-2015 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import de.blinkt.openvpn.R
import de.blinkt.openvpn.core.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {
    val isAndroidTV: Boolean
        get() {
            val uiModeManager = getSystemService(UI_MODE_SERVICE) as UiModeManager
            return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (isAndroidTV) {
            requestWindowFeature(Window.FEATURE_OPTIONS_PANEL)
        }
        this.enableEdgeToEdge(SystemBarStyle.dark(R.color.primary_dark))
        super.onCreate(savedInstanceState)
    }

    fun setUpEdgeEdgeInsetsListener(
        rootView: View,
        contentViewId: Int = R.id.root_linear_layout,
        setupBottom: Boolean = true
    ) {
        val contentView = rootView.findViewById<View>(contentViewId)

        ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, windowInsets ->
            val insets =
                windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val statusbarbg = findViewById<View>(R.id.statusbar_background);

            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())

            statusbarbg.layoutParams.height = statusBarInsets.top


            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = insets.top
            }

            v.updatePadding(
                left = insets.left,
                right = insets.right,
            )
            if (setupBottom) {
                v.updatePadding(bottom = insets.bottom)
                WindowInsetsCompat.CONSUMED
            } else {
                windowInsets
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.updateResources(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LocaleHelper.onConfigurationChange(this)
    }
}
