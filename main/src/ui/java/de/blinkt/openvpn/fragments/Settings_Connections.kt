/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */
package de.blinkt.openvpn.fragments

import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Checkable
import android.widget.ImageButton
import android.widget.TextView
import de.blinkt.openvpn.fragments.Settings_Fragment
import de.blinkt.openvpn.fragments.ConnectionsAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import de.blinkt.openvpn.R

class Settings_Connections : Settings_Fragment(), View.OnClickListener {
    private lateinit var mConnectionsAdapter: ConnectionsAdapter
    private lateinit var mWarning: TextView
    private lateinit var mUseRandomRemote: Checkable
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            inflater.inflate(R.menu.connections, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    )
            : View? {
        val v = inflater.inflate(R.layout.connections, container, false)
        mWarning = v.findViewById<View>(R.id.noserver_active_warning) as TextView
        mRecyclerView = v.findViewById<View>(R.id.connection_recycler_view) as RecyclerView
        mConnectionsAdapter = ConnectionsAdapter(activity, this, mProfile)

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        mRecyclerView.adapter = mConnectionsAdapter
        val fab_button = v.findViewById<View>(R.id.add_new_remote) as ImageButton?
        fab_button?.setOnClickListener(this)
        mUseRandomRemote = v.findViewById<View>(R.id.remote_random) as Checkable
        mUseRandomRemote.isChecked = mProfile.mRemoteRandom
        mConnectionsAdapter.displayWarningIfNoneEnabled()
        return v
    }

    override fun onClick(v: View) {
        if (v.id == R.id.add_new_remote) {
            mConnectionsAdapter.addRemote()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_new_remote) mConnectionsAdapter.addRemote()
        return super.onOptionsItemSelected(item)
    }

    override fun savePreferences() {
        mConnectionsAdapter.saveProfile()
        mProfile.mRemoteRandom = mUseRandomRemote.isChecked
    }

    fun setWarningVisible(showWarning: Int) {
        mWarning.visibility = showWarning
    }
}