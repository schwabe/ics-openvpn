/*
 * Copyright (c) 2012-2020 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import org.jetbrains.anko.runOnUiThread
import java.util.*

internal class AppViewHolder(var rootView : View) : RecyclerView.ViewHolder(rootView) {
    var mInfo: ApplicationInfo? = null
    lateinit var appName: TextView
    lateinit var appIcon: ImageView
    //public TextView appSize;
    //public TextView disabled;
    lateinit var checkBox: CompoundButton

    companion object {

        fun create(inflater: LayoutInflater, parent: ViewGroup): AppViewHolder {
            val view = inflater.inflate(R.layout.allowed_application_layout, parent, false)

            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            val holder = AppViewHolder(view)
            holder.appName = view.findViewById<View>(R.id.app_name) as TextView
            holder.appIcon = view.findViewById<View>(R.id.app_icon) as ImageView
            //holder.appSize = (TextView) convertView.findViewById(R.id.app_size);
            //holder.disabled = (TextView) convertView.findViewById(R.id.app_disabled);
            holder.checkBox = view.findViewById<View>(R.id.app_selected) as CompoundButton
            view.tag = holder


            return holder
        }

        fun createSettingsHolder(inflater: LayoutInflater, parent: ViewGroup): AppViewHolder {

            val settingsView = inflater.inflate(R.layout.allowed_application_settings, parent, false)

            val holder = AppViewHolder(settingsView)
            settingsView.tag = holder
            return holder
        }
    }

}

internal class PackageAdapter(c: Context, vp: VpnProfile) : RecyclerView.Adapter<AppViewHolder>(), Filterable, CompoundButton.OnCheckedChangeListener {
    private val mInflater: LayoutInflater = LayoutInflater.from(c)
    private val mPm: PackageManager = c.packageManager
    private var mPackages: Vector<ApplicationInfo> = Vector()
    private val mFilter = ItemFilter()
    private var mFilteredData: Vector<ApplicationInfo> = mPackages
    private val mProfile = vp

    init {
        setHasStableIds(true)
    }


    fun populateList(c: Context) {
        val installedPackages = mPm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Remove apps not using Internet

        var androidSystemUid = 0
        val apps = Vector<ApplicationInfo>()

        try {
            val system = mPm.getApplicationInfo("android", PackageManager.GET_META_DATA)
            androidSystemUid = system.uid
            apps.add(system)
        } catch (e: PackageManager.NameNotFoundException) {
        }


        for (app in installedPackages) {

            if (mPm.checkPermission(Manifest.permission.INTERNET, app.packageName) == PackageManager.PERMISSION_GRANTED && app.uid != androidSystemUid) {

                apps.add(app)
            }
        }

        Collections.sort(apps, ApplicationInfo.DisplayNameComparator(mPm))
        mPackages = apps
        mFilteredData = apps
        c.runOnUiThread { notifyDataSetChanged() }
    }

    override fun getItemId(position: Int): Long {
        if (position == 0)
            return "settings".hashCode().toLong()
        return mFilteredData[position - 1].packageName.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        if (position == 0) {
            bindSettingsView(holder)
        } else
            bindViewApp(position - 1, holder)

    }
    internal fun changeDisallowText(allowTextView:TextView, selectedAreDisallowed: Boolean) {
        if (selectedAreDisallowed)
            allowTextView.setText(R.string.vpn_disallow_radio)
        else
            allowTextView.setText(R.string.vpn_allow_radio)
    }

    private fun bindSettingsView(holder: AppViewHolder) {
        val  settingsView = holder.rootView
        val allowTextView = settingsView.findViewById<View>(R.id.default_allow_text) as TextView

        val vpnOnDefaultSwitch = settingsView.findViewById<View>(R.id.default_allow) as Switch

        changeDisallowText(allowTextView, mProfile.mAllowedAppsVpnAreDisallowed)
        vpnOnDefaultSwitch.isChecked = mProfile.mAllowedAppsVpnAreDisallowed
        vpnOnDefaultSwitch.setOnCheckedChangeListener { _, isChecked ->
            mProfile.mAllowedAppsVpnAreDisallowed = isChecked
            notifyDataSetChanged()
        }



        val vpnAllowBypassSwitch = settingsView.findViewById<View>(R.id.allow_bypass) as Switch

        vpnAllowBypassSwitch.setOnCheckedChangeListener { _, isChecked -> mProfile.mAllowAppVpnBypass = isChecked }

        vpnAllowBypassSwitch.isChecked = mProfile.mAllowAppVpnBypass
    }

    fun bindViewApp(position: Int, viewHolder: AppViewHolder){
        viewHolder.mInfo = mFilteredData[position]
        val mInfo = mFilteredData[position]


        var appName = mInfo.loadLabel(mPm)

        if (TextUtils.isEmpty(appName))
            appName = mInfo.packageName
        viewHolder.appName.text = appName
        viewHolder.appIcon.setImageDrawable(mInfo.loadIcon(mPm))
        viewHolder.checkBox.tag = mInfo.packageName
        viewHolder.checkBox.setOnCheckedChangeListener(this)


        viewHolder.checkBox.isChecked = mProfile.mAllowedAppsVpn.contains(mInfo.packageName)
    }


    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        val packageName = buttonView.tag as String
        if (isChecked) {
            mProfile.mAllowedAppsVpn.add(packageName)
        } else {
            mProfile.mAllowedAppsVpn.remove(packageName)
        }
    }

    override fun getFilter(): Filter {
        return mFilter
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        if (viewType == 0)
            return AppViewHolder.createSettingsHolder(mInflater, parent)
        else
            return AppViewHolder.create(mInflater, parent)
    }

    override fun getItemCount(): Int {
        return mFilteredData.size + 1

    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    private inner class ItemFilter : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {

            val filterString = constraint.toString().toLowerCase(Locale.getDefault())

            val results = FilterResults()


            val count = mPackages.size
            val nlist = Vector<ApplicationInfo>(count)

            for (i in 0 until count) {
                val pInfo = mPackages[i]
                var appName = pInfo.loadLabel(mPm)

                if (TextUtils.isEmpty(appName))
                    appName = pInfo.packageName

                if (appName is String) {
                    if (appName.toLowerCase(Locale.getDefault()).contains(filterString))
                        nlist.add(pInfo)
                } else {
                    if (appName.toString().toLowerCase(Locale.getDefault()).contains(filterString))
                        nlist.add(pInfo)
                }
            }
            results.values = nlist
            results.count = nlist.size

            return results
        }

        override fun publishResults(constraint: CharSequence, results: FilterResults) {

            mFilteredData = results.values as Vector<ApplicationInfo>
            notifyDataSetChanged()
        }

    }


}