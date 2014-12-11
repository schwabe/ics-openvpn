/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.blinkt.openvpn.R;

public class FaqFragment extends Fragment  {
private static int[] faqitems[] =
        {
                {R.string.faq_howto_title, R.string.faq_howto},
                {R.string.faq_vpndialog43_title, R.string.faq_vpndialog43},
                {R.string.faq_system_dialogs_title, R.string.faq_system_dialogs},
                {R.string.faq_duplicate_notification_title, R.string.faq_duplicate_notification},
                {R.string.faq_androids_clients_title, R.string.faq_android_clients},
                {R.string.battery_consumption_title, R.string.baterry_consumption},
                {R.string.tap_mode, R.string.faq_tap_mode},
                {R.string.vpn_tethering_title, R.string.faq_tethering},
                {R.string.faq_security_title, R.string.faq_security},
                {R.string.broken_images, R.string.broken_images_faq},
                {R.string.faq_shortcut, R.string.faq_howto_shortcut},
                {R.string.tap_mode, R.string.tap_faq2},
                {R.string.copying_log_entries, R.string.faq_copying},
                {R.string.tap_mode, R.string.tap_faq3},
                {R.string.faq_routing_title, R.string.faq_routing}
        };

    private RecyclerView mRecyclerView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	View v= inflater.inflate(R.layout.faq, container, false);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int dpWidth = (int) (displaymetrics.widthPixels /getResources().getDisplayMetrics().density);

        //better way but does not work on 5.0
        //int dpWidth = (int) (container.getWidth()/getResources().getDisplayMetrics().density);
        int columns = dpWidth/360;
        columns = Math.max(1, columns);


        mRecyclerView = (RecyclerView) v.findViewById(R.id.faq_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);


        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(columns, StaggeredGridLayoutManager.VERTICAL));

        mRecyclerView.setAdapter(new FaqViewAdapter(getActivity(), getFAQEntries()));

		return v;
    }

    /* I think the problem mentioned in there should not affect, 4.3+ */
    private int[][] getFAQEntries() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            int[][] newFaqItems = new int[faqitems.length - 1][2];
            int j=0;
            for (int i = 0;i < faqitems.length;i++) {
                if (faqitems[i][0] != R.string.broken_images) {
                    newFaqItems[j] = faqitems[i];
                    j++;
                }
            }
            return newFaqItems;

        } else {
            return faqitems;
        }
    }

}
