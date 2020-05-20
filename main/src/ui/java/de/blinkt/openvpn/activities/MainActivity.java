/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import java.util.Set;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.GraphFragment;
import de.blinkt.openvpn.fragments.LogFragment;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;


public class MainActivity extends BaseActivity {

    private static final String FEATURE_TELEVISION = "android.hardware.type.television";
    private static final String FEATURE_LEANBACK = "android.software.leanback";
    //private TabLayout mTabs;
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);

        if (getIntent().getExtras() != null)
            printBundle(getIntent().getExtras());
        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), this);
        mPagerAdapter.setFragmentArgs(getIntent().getExtras());

        /* Toolbar and slider should have the same elevation */
        disableToolbarElevation();


        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);
        mPagerAdapter.addTab(R.string.graph, GraphFragment.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }


        if (isDirectToTV())
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment.class);

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);

        //mTabs =  findViewById(R.id.sliding_tabs);
        //mTabs.setViewPager(mPager);
    }

    private void printBundle(Bundle extras) {
        Set<String> keys = extras.keySet();
        for (String key : keys) {
            Object o = extras.get(key);
            Log.d("Activity started", key + "=" + o.toString());
        }
    }

    private boolean isDirectToTV() {
        return (getPackageManager().hasSystemFeature(FEATURE_TELEVISION)
                || getPackageManager().hasSystemFeature(FEATURE_LEANBACK));
    }


    private void disableToolbarElevation() {
        ActionBar toolbar = getSupportActionBar();
        toolbar.setElevation(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent() != null) {
            String page = getIntent().getStringExtra("PAGE");
            if ("graph".equals(page)) {
                mPager.setCurrentItem(1);
            }
            setIntent(null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_log) {
            Intent showLog = new Intent(this, LogWindow.class);
            startActivity(showLog);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        System.out.println(data);


    }


}
