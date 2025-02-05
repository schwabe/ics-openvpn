/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.activities

import android.annotation.TargetApi
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ProfileManager
import de.blinkt.openvpn.core.VpnStatus
import de.blinkt.openvpn.fragments.Settings_Allowed_Apps
import de.blinkt.openvpn.fragments.Settings_Authentication
import de.blinkt.openvpn.fragments.Settings_Basic
import de.blinkt.openvpn.fragments.Settings_Connections
import de.blinkt.openvpn.fragments.Settings_IP
import de.blinkt.openvpn.fragments.Settings_Obscure
import de.blinkt.openvpn.fragments.Settings_Routing
import de.blinkt.openvpn.fragments.Settings_UserEditable
import de.blinkt.openvpn.fragments.ShowConfigFragment
import de.blinkt.openvpn.fragments.VPNProfileList
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter

class VPNPreferences : BaseActivity(), VpnStatus.ProfileNotifyListener {
    private var mProfileUUID: String? = null
    private var mProfile: VpnProfile? = null
    private lateinit var mPager: ViewPager2
    private lateinit var mPagerAdapter: ScreenSlidePagerAdapter

    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected fun isValidFragment(fragmentName: String): Boolean {
        for (c in validFragments) if (c.name == fragmentName) return true
        return false
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(intent.getStringExtra("$packageName.profileUUID"), mProfileUUID)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // When a profile is deleted from a category fragment in hadset mod we need to finish
        // this activity as well when returning
        if (mProfile == null || mProfile!!.profileDeleted) {
            setResult(VPNProfileList.RESULT_VPN_DELETED)
            finish()
        }
        if (mProfile!!.mTemporaryProfile) {
            Toast.makeText(this, "Temporary profiles cannot be edited", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private val profile: Unit
        get() {
            val intent = intent

            if (intent != null) {
                var profileUUID = intent.getStringExtra("$packageName.profileUUID")
                if (profileUUID == null) {
                    val initialArguments =
                        getIntent().getBundleExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS)
                    profileUUID = initialArguments!!.getString("$packageName.profileUUID")
                }
                if (profileUUID != null) {
                    mProfileUUID = profileUUID
                    mProfile = ProfileManager.get(this, mProfileUUID)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        mProfileUUID = intent.getStringExtra("$packageName.profileUUID")
        if (savedInstanceState != null) {
            val savedUUID = savedInstanceState.getString("$packageName.profileUUID")
            if (savedUUID != null) mProfileUUID = savedUUID
        }
        super.onCreate(savedInstanceState)

        mProfile = ProfileManager.get(this, mProfileUUID)
        if (mProfile == null) {
            Toast.makeText(this, "Profile to edit cannot be found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        VpnStatus.addProfileStateListener(this);

        title = getString(R.string.edit_profile_title, mProfile!!.name)


        val rootview = layoutInflater.inflate(R.layout.main_activity, null)
        setUpEdgeEdgeInsetsListener(rootview, R.id.root_linear_layout)

        disableToolbarElevation()

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = rootview.findViewById(R.id.pager)
        val tablayout: TabLayout = rootview.findViewById(R.id.tab_layout)
        mPagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager, lifecycle, this)


        val fragmentArguments = Bundle()
        fragmentArguments.putString("$packageName.profileUUID", mProfileUUID)
        mPagerAdapter.setFragmentArgs(fragmentArguments)

        if (mProfile!!.mUserEditable) {
            mPagerAdapter.addTab(R.string.basic, Settings_Basic::class.java)
            mPagerAdapter.addTab(R.string.server_list, Settings_Connections::class.java)
            mPagerAdapter.addTab(R.string.ipdns, Settings_IP::class.java)
            mPagerAdapter.addTab(R.string.routing, Settings_Routing::class.java)
            mPagerAdapter.addTab(R.string.settings_auth, Settings_Authentication::class.java)

            mPagerAdapter.addTab(R.string.advanced, Settings_Obscure::class.java)
        } else {
            mPagerAdapter.addTab(R.string.basic, Settings_UserEditable::class.java)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mPagerAdapter.addTab(R.string.vpn_allowed_apps, Settings_Allowed_Apps::class.java)
        }
        mPagerAdapter.addTab(R.string.generated_config, ShowConfigFragment::class.java)


        mPager.setAdapter(mPagerAdapter)

        //TabBarView tabs = (TabBarView) findViewById(R.id.sliding_tabs);
        //tabs.setViewPager(mPager);

        TabLayoutMediator(tablayout, mPager) { tab, position ->
            tab.text = mPagerAdapter.getPageTitle(position)
        }.attach()

        setContentView(rootview)
    }


    override fun onBackPressed() {
        setResult(RESULT_OK, intent)
        super.onBackPressed()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.remove_vpn) askProfileRemoval()
        if (item.itemId == R.id.duplicate_vpn) {
            val data = Intent()
            data.putExtra(VpnProfile.EXTRA_PROFILEUUID, mProfileUUID)
            setResult(VPNProfileList.RESULT_VPN_DUPLICATE, data)
            finish()
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.vpnpreferences_menu, menu)

        return super.onCreateOptionsMenu(menu)
    }

    private fun askProfileRemoval() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Confirm deletion")
        dialog.setMessage(getString(R.string.remove_vpn_query, mProfile!!.mName))

        dialog.setPositiveButton(
            android.R.string.yes
        ) { dialog1: DialogInterface?, which: Int -> removeProfile(mProfile) }
        dialog.setNegativeButton(android.R.string.no, null)
        dialog.create().show()
    }

    protected fun removeProfile(profile: VpnProfile?) {
        ProfileManager.getInstance(this).removeProfile(this, profile)
        setResult(VPNProfileList.RESULT_VPN_DELETED)
        finish()
    }

    private fun disableToolbarElevation() {
        val toolbar = supportActionBar
        toolbar!!.elevation = 0f
    }

    companion object {
        val validFragments: Array<Class<*>> = arrayOf(
            Settings_Authentication::class.java,
            Settings_Basic::class.java,
            Settings_IP::class.java,
            Settings_Obscure::class.java,
            Settings_Routing::class.java,
            ShowConfigFragment::class.java,
            Settings_Connections::class.java,
            Settings_Allowed_Apps::class.java,
        )
    }

    override fun notifyProfileVersionChanged(
        uuid: String?,
        version: Int,
        changedInThisProcess: Boolean
    ) {
        if (mProfile?.uuidString != uuid)
            return;

        if ((mProfile?.mVersion?: 0) < version)
        {
            /* Profile has changed outside of our process. Most likely from the AIDL service.  */
            Toast.makeText(this, R.string.editor_close_profile_changed, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
