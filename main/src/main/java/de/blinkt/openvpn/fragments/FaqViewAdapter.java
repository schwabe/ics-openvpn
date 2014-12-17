/*
 * Copyright (c) 2012-2014 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.blinkt.openvpn.R;

import static android.support.v7.widget.RecyclerView.ViewHolder;

public class FaqViewAdapter extends RecyclerView.Adapter<FaqViewAdapter.FaqViewHolder> {
    private final int[][] mFaqItems;
    private final Spanned[] mHtmlEntries;

    public FaqViewAdapter(Context context, int[][] faqItems) {
        mFaqItems = faqItems;

        mHtmlEntries = new Spanned[faqItems.length];
        for (int i =0; i < faqItems.length; i++) {
            mHtmlEntries[i] = Html.fromHtml(context.getString(faqItems[i][1]));

            // Add hack R.string.faq_system_dialogs_title -> R.string.faq_system_dialog_xposed
            if (faqItems[i][0] == R.string.faq_system_dialogs_title)
            {
                Spanned xposedtext = Html.fromHtml(context.getString(R.string.faq_system_dialog_xposed));
                mHtmlEntries[i] = (Spanned) TextUtils.concat(mHtmlEntries[i], xposedtext);
            }

        }

    }

    public static class FaqViewHolder extends RecyclerView.ViewHolder {

        private final CardView mView;
        private final TextView mBody;
        private final TextView mHead;

        public FaqViewHolder(View itemView) {
            super(itemView);

            mView = (CardView) itemView;
            mBody = (TextView)mView.findViewById(R.id.faq_body);
            mHead = (TextView)mView.findViewById(R.id.faq_head);
            mBody.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    @Override
    public FaqViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.faqcard, viewGroup, false);
        return new FaqViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FaqViewHolder faqViewHolder, int i) {
        faqViewHolder.mHead.setText(mFaqItems[i][0]);
        faqViewHolder.mBody.setText(mHtmlEntries[i]);
    }

    @Override
    public int getItemCount() {
        return mFaqItems.length;
    }

}
