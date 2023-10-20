/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import de.blinkt.openvpn.R
import de.blinkt.openvpn.fragments.*
import de.blinkt.openvpn.fragments.ImportRemoteConfig.Companion.newInstance
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter

class MainActivity : BaseActivity() {
    private lateinit var mPager: ViewPager
    private lateinit var mPagerAdapter: ScreenSlidePagerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager)
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, this)

        /* Toolbar and slider should have the same elevation */disableToolbarElevation()
        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList::class.java)
        mPagerAdapter.addTab(R.string.graph, GraphFragment::class.java)
        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings::class.java)
        mPagerAdapter.addTab(R.string.faq, FaqFragment::class.java)
        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment::class.java)
        }
        if (isAndroidTV)
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment::class.java)
        mPagerAdapter.addTab(R.string.about, AboutFragment::class.java)
        mPager.setAdapter(mPagerAdapter)
    }

    private fun disableToolbarElevation() {
        supportActionBar?.elevation = 0f
    }

    override fun onResume() {
        super.onResume()
        val intent = intent
        if (intent != null) {
            val action = intent.action
            if (Intent.ACTION_VIEW == action) {
                val uri = intent.data
                uri?.let { checkUriForProfileImport(it) }
            }
            val page = intent.getStringExtra("PAGE")
            if ("graph" == page) {
                mPager.currentItem = 1
            }
            setIntent(null)
        }
    }

    private fun checkUriForProfileImport(uri: Uri) {
        if ("openvpn" == uri.scheme && "import-profile" == uri.host) {
            var realUrl = uri.encodedPath + "?" + uri.encodedQuery
            if (!realUrl.startsWith("/https://")) {
                Toast.makeText(
                    this,
                    "Cannot use openvpn://import-profile/ URL that does not use https://",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            realUrl = realUrl.substring(1)
            startOpenVPNUrlImport(realUrl)
        }
    }

    private fun startOpenVPNUrlImport(url: String) {
        val asImportFrag = newInstance(url)
        asImportFrag.show(supportFragmentManager, "dialog")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.show_log) {
            val showLog = Intent(this, LogWindow::class.java)
            startActivity(showLog)
        }
        return super.onOptionsItemSelected(item)
    }
}