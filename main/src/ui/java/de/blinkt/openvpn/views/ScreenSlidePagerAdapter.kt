/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.views

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.blinkt.openvpn.R
import java.util.Vector

/**
 * Created by arne on 18.11.14.
 */
class ScreenSlidePagerAdapter(fm: FragmentManager, lc: Lifecycle, c: Context) :
    FragmentStateAdapter(
        fm, lc
    ) {
    private val res: Resources = c.resources
    private var mFragmentArguments: Bundle? = null

    fun setFragmentArgs(fragmentArguments: Bundle?) {
        mFragmentArguments = fragmentArguments
    }

    internal class Tab(var fragmentClass: Class<out Fragment>, var mName: String)

    private val mTabs = Vector<Tab>()
    private var mBottomPadding= 0

    override fun createFragment(position: Int): Fragment {
        try {
            val fragment = mTabs[position].fragmentClass.newInstance()
            if (mFragmentArguments != null) fragment.arguments = mFragmentArguments

            return fragment
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
        throw IndexOutOfBoundsException("index wrong")
    }

    fun getPageTitle(position: Int): CharSequence {
        return mTabs[position].mName
    }

    override fun getItemCount(): Int {
        return mTabs.size
    }

    fun addTab(@StringRes name: Int, fragmentClass: Class<out Fragment>) {
        mTabs.add(Tab(fragmentClass, res.getString(name)))
    }

    fun setBottomPadding(bottom: Int) {
        mBottomPadding = bottom;
    }
}
