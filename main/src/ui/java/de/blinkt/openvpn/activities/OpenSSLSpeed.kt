/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView

import java.util.Locale
import java.util.Vector

import de.blinkt.openvpn.R
import de.blinkt.openvpn.core.NativeUtils
import de.blinkt.openvpn.core.OpenVPNService

class OpenSSLSpeed : BaseActivity() {
    private lateinit var  mCipher: EditText
    private lateinit var mAdapter: SpeedArrayAdapter
    private lateinit var mListView: ListView


    internal class SpeedArrayAdapter(private val mContext: Context) : ArrayAdapter<SpeedResult>(mContext, 0) {
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
                        view.findViewById(R.id.blocksintime))
                view.tag = holder
            }

            val holder = view.tag as ViewHolder

            val total = res!!.count * res.length
            val size = OpenVPNService.humanReadableByteCount(res.length.toLong(), false, mContext.resources)

            holder.blocksize.text = size
            holder.ciphername.text = res.algorithm

            if (res.failed) {
                holder.blocksInTime.setText(R.string.openssl_error)
                holder.speed.text = "-"
            } else if (res.running) {
                holder.blocksInTime.setText(R.string.running_test)
                holder.speed.text = "-"
            } else {
                val totalBytes = OpenVPNService.humanReadableByteCount(total.toLong(), false, mContext.resources)
                // TODO: Fix localisation here
                val blockPerSec = OpenVPNService.humanReadableByteCount((total / res.time).toLong(), false, mContext.resources) + "/s"
                holder.speed.text = blockPerSec
                holder.blocksInTime.text = String.format(Locale.ENGLISH, "%d blocks (%s) in %2.1fs", res.count.toLong(), totalBytes, res.time)
            }

            return view

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.openssl_speed)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        findViewById<View>(R.id.testSpecific).setOnClickListener { _ -> runAlgorithms(mCipher.text.toString()) }
        mCipher = findViewById<View>(R.id.ciphername) as EditText

        mListView = findViewById(R.id.results)

        mAdapter = SpeedArrayAdapter(this)
        mListView.adapter = mAdapter

    }

    private fun runAlgorithms(algorithms: String) {
        if (runTestAlgorithms != null)
            runTestAlgorithms!!.cancel(true)
        runTestAlgorithms = SpeeedTest()
        runTestAlgorithms!!.execute(*algorithms.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }


    internal class SpeedResult(var algorithm: String) {
        var failed = false

        var count: Double = 0.toDouble()
        var time: Double = 0.toDouble()
        var length: Int = 0
        var running = true
    }


    private inner class SpeeedTest : AsyncTask<String, SpeedResult, Array<SpeedResult>>() {


        private var mCancel = false

        override fun doInBackground(vararg strings: String): Array<SpeedResult> {
            val mResult = Vector<SpeedResult>()

            for (algorithm in strings) {

                // Skip 16b and 16k as they are not relevevant for VPN
                var i = 1
                while (i < NativeUtils.openSSLlengths.size - 1 && !mCancel) {
                    val result = SpeedResult(algorithm)
                    result.length = NativeUtils.openSSLlengths[i]
                    mResult.add(result)
                    publishProgress(result)
                    val resi = NativeUtils.getOpenSSLSpeed(algorithm, i)
                    if (resi == null) {
                        result.failed = true
                    } else {
                        result.count = resi[1]
                        result.time = resi[2]
                    }
                    result.running = false
                    publishProgress(result)
                    i++
                }
            }

            return mResult.toTypedArray()

        }

        override fun onProgressUpdate(vararg values: SpeedResult) {
            for (r in values) {
                if (r.running)
                    mAdapter.add(r)
                mAdapter.notifyDataSetChanged()
            }
        }

        override fun onPostExecute(speedResult: Array<SpeedResult>) {

        }

        override fun onCancelled(speedResults: Array<SpeedResult>) {
            mCancel = true
        }
    }

    companion object {

        private var runTestAlgorithms: SpeeedTest? = null
    }


}
