/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments;

import android.content.Context;
import android.os.AsyncTask;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.blinkt.openvpn.R;

class FaqViewAdapter(context: Context, faqItems: Array<FAQEntry>) :
    RecyclerView.Adapter<FaqViewAdapter.FaqViewHolder>() {
    private val mFaqItems: Array<FAQEntry>
    private val mHtmlEntries: Array<Spanned?>
    private val mHtmlEntriesTitle: Array<Spanned?>
    private val mContext: Context
    private var loaded = false

    private inner class FetchStrings :
        AsyncTask<FAQEntry?, Void?, Void?>() {
        protected override fun onPostExecute(aVoid: Void) {
            loaded = true
            notifyDataSetChanged()
        }

        override fun doInBackground(vararg params: FAQEntry): Void {
            fetchStrings(params)
            return null
        }
    }

    private fun fetchStrings(faqItems: Array<FAQEntry>) {
        for (i in faqItems.indices) {
            val versionText: String = mFaqItems[i].getVersionsString(mContext)
            var title: String
            var textColor = ""
            title = if (mFaqItems[i].title === -1) "" else mContext.getString(faqItems[i].title)
            if (!mFaqItems[i].runningVersion()) textColor = "<font color=\"gray\">"
            if (versionText != null) {
                mHtmlEntriesTitle[i] = TextUtils.concat(
                    Html.fromHtml(textColor + title),
                    Html.fromHtml("$textColor<br><small>$versionText</small>")
                ) as Spanned
            } else {
                mHtmlEntriesTitle[i] = Html.fromHtml(title)
            }
            val content = mContext.getString(faqItems[i].description)
            mHtmlEntries[i] = Html.fromHtml(textColor + content)

            // Add hack R.string.faq_system_dialogs_title -> R.string.faq_system_dialog_xposed
            if (faqItems[i].title === R.string.faq_system_dialogs_title) {
                val xPosedtext =
                    Html.fromHtml(textColor + mContext.getString(R.string.faq_system_dialog_xposed))
                mHtmlEntries[i] = TextUtils.concat(mHtmlEntries[i], xPosedtext) as Spanned
            }
        }
    }

    class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mView: CardView
        private val mBody: TextView
        private val mHead: TextView

        init {
            mView = itemView as CardView
            mBody = mView.findViewById<View>(R.id.faq_body) as TextView
            mHead = mView.findViewById<View>(R.id.faq_head) as TextView
            mBody.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): FaqViewHolder {
        val view: View = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.faqcard, viewGroup, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(faqViewHolder: FaqViewHolder, i: Int) {
        faqViewHolder.mHead.text = mHtmlEntriesTitle[i]
        faqViewHolder.mBody.text = mHtmlEntries[i]
    }

    override fun getItemCount(): Int {
        return if (loaded) mFaqItems.size else 0
    }

    init {
        mFaqItems = faqItems
        mContext = context
        mHtmlEntries = arrayOfNulls(faqItems.size)
        mHtmlEntriesTitle = arrayOfNulls(faqItems.size)
        FetchStrings().execute(*faqItems)
    }
}
