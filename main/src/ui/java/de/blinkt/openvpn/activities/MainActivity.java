/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities;

import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.fragments.AboutFragment;
import de.blinkt.openvpn.fragments.FaqFragment;
import de.blinkt.openvpn.fragments.GeneralSettings;
import de.blinkt.openvpn.fragments.GraphFragment;
import de.blinkt.openvpn.fragments.ImportRemoteConfig;
import de.blinkt.openvpn.fragments.LogFragment;
import de.blinkt.openvpn.fragments.SendDumpFragment;
import de.blinkt.openvpn.fragments.VPNProfileList;
import de.blinkt.openvpn.views.ScreenSlidePagerAdapter;


public class MainActivity extends BaseActivity {

    private static final String FEATURE_TELEVISION = "android.hardware.type.television";
    private static final String FEATURE_LEANBACK = "android.software.leanback";
    private TabLayout mTabs;
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;

    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);


        // Instantiate a ViewPager and a PagerAdapter.
        mPager = findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), this);

        /* Toolbar and slider should have the same elevation */
        disableToolbarElevation();


        mPagerAdapter.addTab(R.string.vpn_list_title, VPNProfileList.class);
        mPagerAdapter.addTab(R.string.graph, GraphFragment.class);

        mPagerAdapter.addTab(R.string.generalsettings, GeneralSettings.class);
        mPagerAdapter.addTab(R.string.faq, FaqFragment.class);

        if (SendDumpFragment.getLastestDump(this) != null) {
            mPagerAdapter.addTab(R.string.crashdump, SendDumpFragment.class);
        }


        if (isAndroidTV())
            mPagerAdapter.addTab(R.string.openvpn_log, LogFragment.class);

        mPagerAdapter.addTab(R.string.about, AboutFragment.class);
        mPager.setAdapter(mPagerAdapter);
    }


    private void disableToolbarElevation() {
        ActionBar toolbar = getSupportActionBar();
        toolbar.setElevation(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_VIEW.equals(action))
            {
                Uri uri = intent.getData();
                if (uri != null)
                    checkUriForProfileImport(uri);
            }
            String page = intent.getStringExtra("PAGE");
            if ("graph".equals(page)) {
                mPager.setCurrentItem(1);
            }
            setIntent(null);
        }
    }

    private void checkUriForProfileImport(Uri uri) {
        if ("openvpn".equals(uri.getScheme()) && "import-profile".equals(uri.getHost()))
        {
            String realUrl = uri.getEncodedPath() + "?" + uri.getEncodedQuery();
            if (!realUrl.startsWith("/https://"))
            {
                Toast.makeText(this, "Cannot use openvpn://import-profile/ URL that does not use https://", Toast.LENGTH_LONG).show();
                return;
            }
            realUrl = realUrl.substring(1);
            startOpenVPNUrlImport(realUrl);
        }
    }

    private void startOpenVPNUrlImport(String url) {
        ImportRemoteConfig asImportFrag = ImportRemoteConfig.newInstance(url);
        asImportFrag.show(getSupportFragmentManager(), "dialog");
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
