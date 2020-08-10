/*
 * Copyright (c) 2012-2020 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import de.blinkt.openvpn.R
import de.blinkt.openvpn.VpnProfile
import org.json.JSONObject

class InternalWebView : AppCompatActivity() {

    lateinit var webView: WebView
    lateinit var urlTextView: TextView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_internal)

        webView = findViewById(R.id.internal_webview)
        urlTextView = findViewById(R.id.url_textview)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            attachMessageHandler()

        val startData = "Trying to open page at ${intent.data.toString()}"
        webView.loadData(startData,"text/plain","UTF-8");
        webView.loadUrl(intent.data.toString())

        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = VpnProfile.getVersionEnvString(this)

        webView.webViewClient = object: WebViewClient() {
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                urlTextView.text = request?.url?.toString();
                return super.shouldOverrideUrlLoading(view, request)
            }

        }
        urlTextView.text =  intent.data.toString()

        setTitle(R.string.internal_web_view)

    }

    @JavascriptInterface
    fun postMessage(json: String?, transferList: String?): Boolean {
        val jObejct = JSONObject(json)

        val action = jObejct.getString("type")
        Log.i("OpenVPN,InternalWebview", json + " ---- " + transferList)

        if (action == "ACTION_REQUIRED") {
            // Should show the hidden webview, nothing for us to do
            return true
        }

        if (action == "CONNECT_SUCCESS" || action == "CONNECT_FAILED") {
            runOnUiThread({finish()})
        }

        /* runOnUiThread({
            Toast.makeText(this, json + " ---- " + transferList, Toast.LENGTH_LONG).show()
        }) */
        return true
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun attachMessageHandler() {
        webView.addJavascriptInterface(this, "appEvent")
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

    }
}