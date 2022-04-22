/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ProfileManager

/**
 * Created by arne on 16.11.14.
 */
class Settings_Allowed_Apps : Fragment(), AdapterView.OnItemClickListener, View.OnClickListener {
    private lateinit var mListView: RecyclerView
    private lateinit var mProfile: VpnProfile
    private lateinit var packageAdapter: PackageAdapter
    private lateinit var mSettingsView: View


    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val avh = view.tag as AppViewHolder
        avh.checkBox.toggle()
    }

    override fun onClick(v: View) {

    }

    override fun onResume() {
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val profileUuid = requireArguments().getString(requireActivity().packageName + ".profileUUID")
        mProfile = ProfileManager.get(activity, profileUuid)
        requireActivity().title = getString(R.string.edit_profile_title, mProfile.name)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.allowed_apps, menu)

        val searchView = menu.findItem(R.id.app_search_widget).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                packageAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(query: String): Boolean {
                packageAdapter.filter.filter(query)
                //mListView.setFilterText(newText)
                //mListView.isTextFilterEnabled = !TextUtils.isEmpty(newText)

                return true
            }
        })
        searchView.setOnCloseListener {
            //mListView.clearTextFilter()
            packageAdapter.filter.filter("")
            false
        }

        super.onCreateOptionsMenu(menu, inflater)
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.allowed_vpn_apps, container, false)

        mListView = v.findViewById<View>(R.id.app_recycler_view) as RecyclerView

        packageAdapter = PackageAdapter(requireContext(), mProfile)
        mListView.setHasFixedSize(true)
        mListView.adapter = packageAdapter

        Thread(Runnable {
            packageAdapter.populateList(requireActivity())
            activity?.runOnUiThread({
                (v.findViewById<View>(R.id.loading_container)).visibility = View.GONE
                (v.findViewById<View>(R.id.app_recycler_view)).visibility = View.VISIBLE
            })
        }).start()

        return v
    }

}
