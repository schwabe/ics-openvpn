/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import de.blinkt.openvpn.R
import de.blinkt.openvpn.core.NativeUtils
import de.blinkt.openvpn.core.OpenVPNService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class OpenSSLSpeed : BaseActivity() {
    private lateinit var mCipher: EditText
    private lateinit var mAdapter: SpeedArrayAdapter
    private lateinit var mListView: ListView


    internal class SpeedArrayAdapter(private val mContext: Context) :
        ArrayAdapter<SpeedResult>(mContext, 0) {
        private val mInflater: LayoutInflater

        init {
            mInflater = LayoutInflater.from(mContext)

        }

        internal data class ViewHolder(
            var ciphername: TextView,
            var speed: TextView,
            var blocksize: TextView,
            var blocksInTime: TextView
        )

        override fun getView(position: Int, v: View?, parent: ViewGroup): View {
            var view = v
            val res = getItem(position)
            if (view == null) {
                view = mInflater.inflate(R.layout.speedviewitem, parent, false)!!
                val holder = ViewHolder(
                    view.findViewById(R.id.ciphername),
                    view.findViewById(R.id.speed),
                    view.findViewById(R.id.blocksize),
                    view.findViewById(R.id.blocksintime)
                )
                view.tag = holder
            }

            val holder = view.tag as ViewHolder

            val total = res!!.count * res.length
            val size = OpenVPNService.humanReadableByteCount(
                res.length.toLong(),
                false,
                mContext.resources
            )

            holder.blocksize.text = size
            holder.ciphername.text = res.algorithm

            if (res.failed) {
                holder.blocksInTime.setText(R.string.openssl_error)
                holder.speed.text = "-"
            } else if (res.running) {
                holder.blocksInTime.setText(R.string.running_test)
                holder.speed.text = "-"
            } else {
                val totalBytes =
                    OpenVPNService.humanReadableByteCount(total.toLong(), false, mContext.resources)
                // TODO: Fix localisation here
                val blockPerSec = OpenVPNService.humanReadableByteCount(
                    (total / res.time).toLong(),
                    false,
                    mContext.resources
                ) + "/s"
                holder.speed.text = blockPerSec
                holder.blocksInTime.text = String.format(
                    Locale.ENGLISH,
                    "%d blocks (%s) in %2.1fs",
                    res.count.toLong(),
                    totalBytes,
                    res.time
                )
            }

            return view

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.openssl_speed)
        setUpEdgeEdgeInsetsListener(getWindow().getDecorView().getRootView(), R.id.speed_root)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        findViewById<View>(R.id.testSpecific).setOnClickListener { _ -> runAlgorithms(mCipher.text.toString()) }
        mCipher = findViewById<View>(R.id.ciphername) as EditText

        mListView = findViewById(R.id.results)

        mAdapter = SpeedArrayAdapter(this)
        mListView.adapter = mAdapter

    }

    private fun runAlgorithms(algorithms: String) {
        lifecycleScope.launch {
            runSpeedTest(algorithms)
        }
    }


    internal class SpeedResult(var algorithm: String) {
        var failed = false

        var count: Double = 0.toDouble()
        var time: Double = 0.toDouble()
        var length: Int = 0
        var running = true
    }

    internal suspend fun showResults(vararg values: SpeedResult) {
        withContext(Dispatchers.Main) {
            for (r in values) {
                if (r.running)
                    mAdapter.add(r)
                mAdapter.notifyDataSetChanged()
            }
        }
    }

    suspend fun runSpeedTest(algorithms: String) {
        withContext(Dispatchers.IO)
        {
            val mResult = Vector<SpeedResult>()

            for (algorithm in algorithms.split(" ")) {
                // Skip 16b and 16k as they are not relevevant for VPN
                var i = 1
                while (i < NativeUtils.openSSLlengths.size - 1) {
                    val result = SpeedResult(algorithm)
                    result.length = NativeUtils.openSSLlengths[i]
                    mResult.add(result)
                    showResults(result)
                    val resi = NativeUtils.getOpenSSLSpeed(algorithm, i)
                    if (resi == null) {
                        result.failed = true
                    } else {
                        result.count = resi[1]
                        result.time = resi[2]
                    }
                    result.running = false
                    showResults(result)
                    i++
                }
            }
        }
    }

}
