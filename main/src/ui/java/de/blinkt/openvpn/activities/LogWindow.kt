/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.os.Bundle
import android.view.MenuItem
import de.blinkt.openvpn.R
import de.blinkt.openvpn.fragments.LogFragment

/**
 * Created by arne on 13.10.13.setUpEdgeEdgeStuff
 */
class LogWindow : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_window)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, LogFragment())
                .commit()
        }

        setUpEdgeEdgeInsetsListener(getWindow().getDecorView().getRootView(), R.id.container)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
