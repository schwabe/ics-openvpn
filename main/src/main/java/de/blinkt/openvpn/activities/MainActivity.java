/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.support.v4n.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;
import de.blinkt.openvpn.views.SlidingTabLayout;
import de.blinkt.openvpn.views.TabBarView;


public class MainActivity extends Activity {

    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;
    private SlidingTabLayout mSlidingTabLayout;

    protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager(), this);

        /* Toolbar and slider should have the same elevation */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            disableToolbarElevation();
        }



        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if(SendDumpFragment.getLastestDump(this)!=null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }

        //mPagerAdapter.addTab(R.string.openvpn_log, LogFragment.class);

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);

        TabBarView tabs = (TabBarView) findViewById(R.id.sliding_tabs);
        tabs.setViewPager(mPager);
	}

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void disableToolbarElevation() {
        ActionBar toolbar = getActionBar();
        toolbar.setElevation(0);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.show_log){
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
