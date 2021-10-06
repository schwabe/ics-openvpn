/*
 * Copyright (c) 2012-2019 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package de.blinkt.openvpn.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.TrafficStats
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Base64.NO_WRAP
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import de.blinkt.openvpn.R
import de.blinkt.openvpn.activities.ConfigConverter
import de.blinkt.openvpn.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Handshake.Companion.handshake
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.internal.tls.OkHostnameVerifier
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.net.ssl.*

class BasicAuthInterceptor(user: String, password: String) : Interceptor {

    private val credentials: String

    init {
        this.credentials = Credentials.basic(user, password)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val authenticatedRequest = request.newBuilder()
                .header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }

}


fun getCompositeSSLSocketFactory(certPin: CertificatePinner, hostname: String): Pair<SSLSocketFactory, X509TrustManager> {
    val trustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return emptyArray()
        }

        @Throws(CertificateException::class)
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            throw CertificateException("Why would we check client certificates?!")
        }

        @Throws(CertificateException::class)
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            certPin.check(hostname, chain.toList())
        }
    }
    val trustPinnedCerts = arrayOf<TrustManager>(trustManager)

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustPinnedCerts, java.security.SecureRandom())
    // Create an ssl socket factory with our all-trusting manager

    return Pair(sslContext.socketFactory, trustManager)

}

class ImportRemoteConfig : DialogFragment() {
    private lateinit var asUseAutologin: CheckBox
    private lateinit var asServername: EditText
    private lateinit var asUsername: EditText
    private lateinit var asPassword: EditText
    private lateinit var dialogView: View

    private lateinit var importChoiceGroup: RadioGroup
    private lateinit var importChoiceAS: RadioButton



    internal fun getHostNameVerifier(prefs: SharedPreferences): HostnameVerifier {
        val pinnedHostnames: Set<String> = prefs.getStringSet("pinnedHosts", emptySet())!!

        val mapping = mutableMapOf<String, String?>()

        pinnedHostnames.forEach { ph ->
            mapping[ph] = prefs.getString("pin-${ph}", "")
        }

        val defaultVerifier = OkHostnameVerifier;
        val pinHostVerifier = object : HostnameVerifier {
            override fun verify(hostname: String, session: SSLSession): Boolean {
                val unverifiedHandshake = session.handshake()
                val cert = unverifiedHandshake.peerCertificates[0] as X509Certificate
                val hostPin = CertificatePinner.pin(cert)

                if (mapping.containsKey(hostname) && mapping[hostname] == hostPin)
                    return true
                else
                    return defaultVerifier.verify(hostname, session)
            }

        }
        return pinHostVerifier
    }

    internal fun buildHttpClient(c: Context, user: String, password: String, hostname: String): OkHttpClient {

        // TODO: HACK
        val THREAD_ID = 10000;
        TrafficStats.setThreadStatsTag(THREAD_ID);

        val prefs = c.getSharedPreferences("pinnedCerts", Context.MODE_PRIVATE)
        val pinnedHosts: Set<String> = prefs.getStringSet("pinnedHosts", emptySet())!!

        val okHttpClient = OkHttpClient.Builder()
        if (user.isNotBlank() && password.isNotBlank()) {
            okHttpClient.addInterceptor(BasicAuthInterceptor(user, password))
        }
        okHttpClient.connectTimeout(15, TimeUnit.SECONDS)

        /* Rely on system certificates if we do not have the host pinned */
        if (pinnedHosts.contains(hostname)) {
            val cpb = CertificatePinner.Builder()

            pinnedHosts.forEach { ph ->
                cpb.add(ph, prefs.getString("pin-${ph}", "")!!)
            }


            val certPinner = cpb.build()
            getCompositeSSLSocketFactory(certPinner, hostname).let {
                okHttpClient.sslSocketFactory(it.first, it.second)
            }
            //okHttpClient.certificatePinner(certPinner)
        }

        okHttpClient.hostnameVerifier(getHostNameVerifier(prefs))

        val client = okHttpClient.build()
        return client

    }

    /**
     * @param fp Fingerprint in sha 256 format
     */
    internal fun addPinnedCert(c: Context, host: String, fp: String) {
        val prefs = c.getSharedPreferences("pinnedCerts", Context.MODE_PRIVATE)
        val pedit = prefs.edit()
        val pinnedHosts: MutableSet<String> = prefs.getStringSet("pinnedHosts", mutableSetOf<String>())!!
            .toMutableSet()

        pinnedHosts.add(host)

        pedit.putString("pin-${host}", "sha256/${fp}")

        pedit.putStringSet("pinnedHosts", pinnedHosts)

        pedit.apply()
    }

    internal fun removedPinnedCert(c: Context, host: String) {
        val prefs = c.getSharedPreferences("pinnedCerts", Context.MODE_PRIVATE)
        val pedit = prefs.edit()
        val pinnedHosts: MutableSet<String> = prefs.getStringSet("pinnedHosts", mutableSetOf<String>())!!.toMutableSet()

        pinnedHosts.remove(host)

        pedit.remove("pin-${host}")

        pedit.putStringSet("pinnedHosts", pinnedHosts)

        pedit.apply()
    }

    fun fetchProfile(c: Context, asUri: HttpUrl, user: String, password: String): Response? {


        val httpClient = buildHttpClient(c, user, password, asUri.host ?: "")

        val request = Request.Builder()
                .url(asUri)
                .build()

        val response = httpClient.newCall(request).execute()

        return response

    }

    /**
     * Returns a new [HttpUrl] representing the URL that is is going to be imported.
     *
     * @throws IllegalArgumentException If this is not a well-formed HTTP or HTTPS URL.
     */
    private fun getAsUrl(url: String, autologin: Boolean): HttpUrl{
        var asurl = url
        if (!asurl.startsWith("http"))
            asurl = "https://" + asurl

        if (autologin)
            asurl += "/rest/GetAutologin?tls-cryptv2=1"
        else
            asurl += "/rest/GetUserlogin?tls-cryptv2=1"

        val asUri = asurl.toHttpUrl()
        return asUri
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return dialogView
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        dialogView = inflater.inflate(R.layout.import_remote_config, null);

        val builder = AlertDialog.Builder(requireContext())

        builder.setView(dialogView)
        builder.setTitle(R.string.import_from_as)

        asServername = dialogView.findViewById(R.id.as_servername)
        asUsername = dialogView.findViewById(R.id.username)
        asPassword = dialogView.findViewById(R.id.password)
        asUseAutologin = dialogView.findViewById(R.id.request_autologin)

        importChoiceGroup = dialogView.findViewById(R.id.import_source_group)
        importChoiceAS = dialogView.findViewById(R.id.import_choice_as)

        importChoiceGroup.setOnCheckedChangeListener { group, checkedId ->
            if (checkedId == R.id.import_choice_as) {
                asServername.setHint(R.string.as_servername)
                asUseAutologin.visibility = View.VISIBLE
            }
            else {
                asServername.setHint(R.string.server_url)
                asUseAutologin.visibility = View.GONE
            }
        }

        builder.setPositiveButton(R.string.import_config, null)
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }

        if (arguments?.getString("url") != null)
        {
            asServername.setText(arguments?.getString("url"))
            importChoiceGroup.check(R.id.import_choice_url)
        }

        val dialog = builder.create()

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog!!.setOnShowListener() { d2 ->
            val d: AlertDialog = d2 as AlertDialog

            d.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener()

            { _ ->
                try {
                    // Check if the URL that being built can be actually be parsed
                    getImportUrl();

                    viewLifecycleOwner.lifecycleScope.launch {
                        doAsImport(asUsername.text.toString(), asPassword.text.toString())
                    }
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(context, "URL is invalid: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val crvMessage = Pattern.compile(".*<Message>CRV1:R,E:(.*):(.*):(.*)</Message>.*", Pattern.DOTALL)


    private fun getImportUrl(): HttpUrl {
        if (importChoiceAS.isChecked)
            return getAsUrl(asServername.text.toString(), asUseAutologin.isChecked)
        else
            return asServername.text.toString().toHttpUrl()
    }


    suspend internal fun doAsImport(user: String, password: String) {
        var pleaseWait:AlertDialog?
        withContext(Dispatchers.IO)
        {

            withContext(Dispatchers.Main)
            {
                val ab = AlertDialog.Builder(requireContext())
                ab.setTitle("Downloading profile")
                ab.setMessage("Please wait")
                pleaseWait = ab.show()

                Toast.makeText(context, "Downloading profile", Toast.LENGTH_LONG).show()
            }

            val asProfileUri:HttpUrl = getImportUrl()


            var e: Exception? = null
            try {
                val response = fetchProfile(requireContext(), asProfileUri, user, password)

                if (response == null) {
                    throw Exception("No Response from Server")
                }

                val profile = response.body?.string()
                if (response.code == 401 && crvMessage.matcher(profile).matches()) {
                    withContext(Dispatchers.Main) {
                        pleaseWait?.dismiss()
                        showCRDialog(profile!!)
                    }
                } else if (response.isSuccessful) {

                    withContext(Dispatchers.Main) {
                        pleaseWait?.dismiss()
                        val startImport = Intent(activity, ConfigConverter::class.java)
                        startImport.action = ConfigConverter.IMPORT_PROFILE_DATA
                        startImport.putExtra(Intent.EXTRA_TEXT, profile)
                        startActivity(startImport)
                        dismiss()
                    }
                } else {
                    throw Exception("Invalid Response from server: \n${response.code} ${response.message} \n\n ${profile}")
                }

            } catch (ce: SSLHandshakeException) {
                e = ce
                // Find out if we are in the non trust path
                if (ce.cause is CertificateException && ce.cause != null) {
                    val certExp: CertificateException = (ce.cause as CertificateException)
                    if (certExp.cause is CertPathValidatorException && certExp.cause != null) {
                        val caPathExp: CertPathValidatorException = certExp.cause as CertPathValidatorException
                        if (caPathExp.certPath.type.equals("X.509") && caPathExp.certPath.certificates.size > 0) {
                            val firstCert: X509Certificate = (caPathExp.certPath.certificates[0] as X509Certificate)

                            val fpBytes = MessageDigest.getInstance("SHA-256").digest(firstCert.publicKey.encoded)
                            val fp = Base64.encodeToString(fpBytes, NO_WRAP)



                            Log.i("OpenVPN", "Found cert with FP ${fp}: ${firstCert.subjectDN}")
                            withContext(Dispatchers.Main) {

                                pleaseWait?.dismiss()

                                AlertDialog.Builder(requireContext())
                                        .setTitle("Untrusted certificate found")
                                        .setMessage(firstCert.toString())
                                        .setPositiveButton("Trust") { _, _ -> addPinnedCert(requireContext(),
                                            asProfileUri.host, fp) }
                                        .setNegativeButton("Do not trust", null)
                                        .show()
                            }
                            e = null
                        }
                    } else if (ce.message != null && ce.message!!.contains("Certificate pinning failure")) {
                        withContext(Dispatchers.Main) {
                            pleaseWait?.dismiss()

                            AlertDialog.Builder(requireContext())
                                    .setTitle("Different certificate than trusted certificate from server")
                                    .setMessage(ce.message)
                                    .setNegativeButton(android.R.string.ok, null)
                                    .setPositiveButton("Forget pinned certificate", { _, _ -> removedPinnedCert(requireContext(),
                                        asProfileUri.host
                                    ) })
                                    .show();
                        }
                        e = null

                    }
                }
            } catch (ge: Exception) {
                e = ge
            }
            if (e != null) {
                withContext(Dispatchers.Main) {
                    pleaseWait?.dismiss()
                    AlertDialog.Builder(requireContext())
                            .setTitle("Import failed")
                            .setMessage("Error: " + e.localizedMessage)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                }
            }
        }
    }

    private fun showCRDialog(response: String) {
        // This is a dirty hack instead of properly parsing the response
        val m = crvMessage.matcher(response)
        // We already know that it matches
        m.matches()
        val challenge = m.group(1)
        var username = m.group(2)
        val message = m.group(3)

        username = String(Base64.decode(username, Base64.DEFAULT))

        val pwprefix = "CRV1::${challenge}::"

        val entry = EditText(context)
        entry.setInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)

        AlertDialog.Builder(requireContext())
                .setTitle("Server request challenge/response authentication")
                .setMessage("Challenge: " + message)
                .setView(entry)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.import_config) { _,_ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        doAsImport(username, pwprefix + entry.text.toString())
                    }
                }
                .show()

    }


    override fun onResume() {
        super.onResume()
        if (arguments == null) {
            asServername.setText(
                Preferences.getDefaultSharedPreferences(activity).getString("as-hostname", "")
            )
            asUsername.setText(
                Preferences.getDefaultSharedPreferences(activity).getString("as-username", "")
            )
            if (Preferences.getDefaultSharedPreferences(activity).getBoolean("as-selected", true)) {
                importChoiceGroup.check(R.id.import_choice_as)
            } else {
                importChoiceGroup.check(R.id.import_choice_url)
            }

        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = Preferences.getDefaultSharedPreferences(activity)
        val editor = prefs.edit()
        editor.putString("as-hostname", asServername.text.toString())
        editor.putString("as-username", asUsername.text.toString())
        editor.putBoolean("as-selected", importChoiceAS.isChecked)
        editor.apply()
    }

    companion object {
        @JvmStatic
        fun newInstance(url:String? = null): ImportRemoteConfig {
            val frag = ImportRemoteConfig()
            if (url != null)
            {
                val extras = Bundle()
                extras.putString("url", url)
                frag.arguments = extras
            }
            return frag
        }
    }
}
