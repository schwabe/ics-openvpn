/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v4n.app.FragmentStatePagerAdapter;
import android.support.v4n.view.ViewPager;

import java.util.Vector;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.PagerSlidingTabStrip;
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
        mPagerAdapter = new ScreenSlidePagerAdapter(getFragmentManager());





        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if(SendDumpFragment.getLastestDump(this)!=null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);

        /*mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.slding_tabs);
        mSlidingTabLayout.setViewPager(mPager); */

        TabBarView tabs = (TabBarView) findViewById(R.id.sliding_tabs);
        tabs.setViewPager(mPager);

        /*
        if (false) {
            Tab logtab = bar.newTab().setText("Log");
            logtab.setTabListener(new TabListener<LogFragment>("log", LogFragment.class));
            bar.addTab(logtab);
        }*/


		
	}

    class Tab {
        public Class<? extends Fragment> fragmentClass;
        String mName;

        public Tab(Class<? extends Fragment> fClass, @StringRes String name){
            mName = name;
            fragmentClass = fClass;
        }

    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        private Vector<Tab> mTabs = new Vector<Tab>();

        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            try {
                return mTabs.get(position).fragmentClass.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return  null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabs.get(position).mName;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        public void addTab(@StringRes int name, Class<? extends Fragment> fragmentClass) {
            mTabs.add(new Tab(fragmentClass, getString(name)));
        }
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		System.out.println(data);


	}


}
