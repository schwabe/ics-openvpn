package de.blinkt.openvpn;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import de.blinkt.openvpn.fragments.*;


public class MainActivity extends Activity {

	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		Tab vpnListTab = bar.newTab().setText(R.string.vpn_list_title);
		Tab generalTab = bar.newTab().setText(R.string.generalsettings);
		Tab faqtab = bar.newTab().setText(R.string.faq);
		Tab abouttab = bar.newTab().setText(R.string.about);

		vpnListTab.setTabListener(new TabListener<VPNProfileList>("profiles", VPNProfileList.class));
		generalTab.setTabListener(new TabListener<GeneralSettings>("settings", GeneralSettings.class));
		faqtab.setTabListener(new TabListener<FaqFragment>("faq", FaqFragment.class));
		abouttab.setTabListener(new TabListener<AboutFragment>("about", AboutFragment.class));

		bar.addTab(vpnListTab);
		bar.addTab(generalTab);
		bar.addTab(faqtab);
		bar.addTab(abouttab);

        if (false) {
            Tab logtab = bar.newTab().setText("Log");
            logtab.setTabListener(new TabListener<LogFragment>("log", LogFragment.class));
            bar.addTab(logtab);
        }

        if(SendDumpFragment.getLastestDump(this)!=null) {
			Tab sendDump = bar.newTab().setText(R.string.crashdump);
			sendDump.setTabListener(new TabListener<SendDumpFragment>("crashdump",SendDumpFragment.class));
			bar.addTab(sendDump);
		}
		
	}

	protected class TabListener<T extends Fragment> implements ActionBar.TabListener
	{
		private Fragment mFragment;
		private String mTag;
		private Class<T> mClass;

        public TabListener(String tag, Class<T> clz) {
            mTag = tag;
            mClass = clz;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }
      
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(MainActivity.this, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }


		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		System.out.println(data);


	}


}
