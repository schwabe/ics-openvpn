package pro.sargo.openvpn

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import de.blinkt.openvpn.api.APIVpnProfile
import de.blinkt.openvpn.api.IOpenVPNAPIService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VpnProfileImporterTest {

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var service: IOpenVPNAPIService

    private lateinit var context: Context
    private lateinit var importer: VpnProfileImporter

    /**
     * Synchronous executor that runs submitted runnables on the calling thread.
     * This makes URI-based import paths deterministic in unit tests.
     */
    private val synchronousExecutor = Executor { it.run() }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        importer = VpnProfileImporter(context, executor = synchronousExecutor)
    }

    @Test
    fun importFromString_returnsProfileOnSuccess() {
        val expectedProfile = APIVpnProfile("uuid-1", "Office VPN", false, null)
        `when`(service.addNewVPNProfile(anyString(), anyBoolean(), anyString()))
            .thenReturn(expectedProfile)

        val result = importer.importFromString(service, "Office VPN", "client\ndev tun")

        assertTrue(result is VpnProfileImporter.ImportResult.Success)
        assertSame(expectedProfile, (result as VpnProfileImporter.ImportResult.Success).profile)
    }

    @Test
    fun importFromString_returnsErrorWhenProfileIsNull() {
        `when`(service.addNewVPNProfile(anyString(), anyBoolean(), anyString()))
            .thenReturn(null)

        val result = importer.importFromString(service, "Office VPN", "client")

        assertTrue(result is VpnProfileImporter.ImportResult.Error)
        assertEquals(
            "OpenVPN API returned null profile",
            (result as VpnProfileImporter.ImportResult.Error).message
        )
    }

    @Test
    fun importFromString_returnsErrorOnException() {
        `when`(service.addNewVPNProfile(anyString(), anyBoolean(), anyString()))
            .thenThrow(RuntimeException("AIDL failure"))

        val result = importer.importFromString(service, "Office VPN", "client")

        assertTrue(result is VpnProfileImporter.ImportResult.Error)
        assertTrue((result as VpnProfileImporter.ImportResult.Error).message.contains("AIDL failure"))
    }

    @Test
    fun importFromString_passesCorrectArguments() {
        val nameCaptor = ArgumentCaptor.forClass(String::class.java)
        val configCaptor = ArgumentCaptor.forClass(String::class.java)
        val editableCaptor = ArgumentCaptor.forClass(Boolean::class.java)

        importer.importFromString(service, "Office VPN", "inline config")

        verify(service).addNewVPNProfile(nameCaptor.capture(), editableCaptor.capture(), configCaptor.capture())
        assertEquals("Office VPN", nameCaptor.value)
        assertEquals("inline config", configCaptor.value)
        assertFalse(editableCaptor.value)
    }

    @Test
    fun shutdown_canBeCalledSafely() {
        importer.shutdown()
    }

    @Test
    fun shutdown_cancelsActiveImportsAndShutsDownExecutor() {
        val executorService = Executors.newSingleThreadExecutor()
        val importerWithExecutor = VpnProfileImporter(context, executor = executorService)

        importerWithExecutor.shutdown()

        assertTrue(executorService.isShutdown)
    }

    @Test
    fun removeProfile_returnsTrueOnSuccess() {
        assertTrue(importer.removeProfile(service, "uuid-1"))
        verify(service).removeProfile("uuid-1")
    }

    @Test
    fun removeProfile_returnsFalseOnException() {
        val serviceThatThrows = mock(IOpenVPNAPIService::class.java)
        `when`(serviceThatThrows.removeProfile(anyString()))
            .thenThrow(RuntimeException("Remove failed"))

        assertFalse(importer.removeProfile(serviceThatThrows, "uuid-1"))
    }

    @Test
    fun importFromUri_readsContentAndImportsProfile() {
        val config = "client\ndev tun\nremote 10.0.0.1"
        val uri = Uri.parse("content://pro.sargo.launcher/config/office.ovpn")
        `when`(contentResolver.openInputStream(uri))
            .thenReturn(ByteArrayInputStream(config.toByteArray()))

        val expectedProfile = APIVpnProfile("uuid-2", "Office VPN", false, null)
        `when`(service.addNewVPNProfile(anyString(), anyBoolean(), anyString()))
            .thenReturn(expectedProfile)

        val importerWithResolver = VpnProfileImporter(
            ContextWithResolver(context, contentResolver),
            executor = synchronousExecutor
        )

        val capturedResults = mutableListOf<VpnProfileImporter.ImportResult>()
        val capturedSteps = mutableListOf<String>()
        var started = false

        importerWithResolver.importFromUri(service, "Office VPN", uri, object : VpnProfileImporter.ImportProgressListener {
            override fun onImportStarted() {
                started = true
            }

            override fun onImportProgress(step: String) {
                capturedSteps.add(step)
            }

            override fun onImportCompleted(result: VpnProfileImporter.ImportResult) {
                capturedResults.add(result)
            }
        })

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue(started)
        assertTrue(capturedSteps.isNotEmpty())

        val result = capturedResults.first()
        assertTrue(result is VpnProfileImporter.ImportResult.Success)
        assertSame(expectedProfile, (result as VpnProfileImporter.ImportResult.Success).profile)
    }

    @Test
    fun importFromUri_returnsErrorWhenUriCannotBeRead() {
        val uri = Uri.parse("content://pro.sargo.launcher/config/missing.ovpn")
        `when`(contentResolver.openInputStream(uri))
            .thenReturn(null)

        val importerWithResolver = VpnProfileImporter(
            ContextWithResolver(context, contentResolver),
            executor = synchronousExecutor
        )

        val capturedResults = mutableListOf<VpnProfileImporter.ImportResult>()

        importerWithResolver.importFromUri(service, "Office VPN", uri, object : VpnProfileImporter.ImportProgressListener {
            override fun onImportStarted() {}
            override fun onImportProgress(step: String) {}
            override fun onImportCompleted(result: VpnProfileImporter.ImportResult) {
                capturedResults.add(result)
            }
        })

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val result = capturedResults.first()
        assertTrue(result is VpnProfileImporter.ImportResult.Error)
        assertTrue((result as VpnProfileImporter.ImportResult.Error).message.contains("Could not read config"))
    }

    @Test
    fun importFromUri_returnsErrorWhenContentResolverThrows() {
        val uri = Uri.parse("content://pro.sargo.launcher/config/broken.ovpn")
        `when`(contentResolver.openInputStream(uri))
            .thenThrow(SecurityException("Permission denied"))

        val importerWithResolver = VpnProfileImporter(
            ContextWithResolver(context, contentResolver),
            executor = synchronousExecutor
        )

        val capturedResults = mutableListOf<VpnProfileImporter.ImportResult>()

        importerWithResolver.importFromUri(service, "Office VPN", uri, object : VpnProfileImporter.ImportProgressListener {
            override fun onImportStarted() {}
            override fun onImportProgress(step: String) {}
            override fun onImportCompleted(result: VpnProfileImporter.ImportResult) {
                capturedResults.add(result)
            }
        })

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val result = capturedResults.first()
        assertTrue(result is VpnProfileImporter.ImportResult.Error)
        assertTrue((result as VpnProfileImporter.ImportResult.Error).message.contains("Could not read config"))
    }

    /**
     * Wraps a real [Context] with a custom [ContentResolver] so URI-based import paths can be
     * exercised without mocking the entire Context.
     */
    private class ContextWithResolver(base: Context, private val resolver: ContentResolver) : android.content.ContextWrapper(base) {
        override fun getContentResolver(): ContentResolver = resolver
    }
}
