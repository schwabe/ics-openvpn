package pro.sargo.openvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenVpnConfigPostProcessorTest {

    private val processor = OpenVpnConfigPostProcessor()

    @Test
    fun process_injectsInlineAuthUserPass() {
        val config = baseConfig().copy(
            vpnUsername = "alice",
            vpnPassword = "wonderland"
        )

        val result = processor.process("client\ndev tun\nremote 1.2.3.4", config)

        assertTrue(result.contains("<auth-user-pass>"))
        assertTrue(result.contains("alice"))
        assertTrue(result.contains("wonderland"))
        assertTrue(result.contains("</auth-user-pass>"))
    }

    @Test
    fun process_replacesExistingAuthUserPass() {
        val config = baseConfig().copy(
            vpnUsername = "bob",
            vpnPassword = "builder"
        )

        val result = processor.process(
            "auth-user-pass /sdcard/creds.txt\nclient\nremote 1.2.3.4",
            config
        )

        assertFalse(result.contains("/sdcard/creds.txt"))
        assertTrue(result.contains("<auth-user-pass>"))
        assertTrue(result.contains("bob"))
        assertTrue(result.contains("builder"))
    }

    @Test
    fun process_doesNotTouchAuthUserPassVerify() {
        val config = baseConfig().copy(vpnUsername = "u", vpnPassword = "p")

        val result = processor.process(
            "auth-user-pass-verify /script.sh via-env\nclient\nremote 1.2.3.4",
            config
        )

        assertTrue(result.contains("auth-user-pass-verify /script.sh via-env"))
        assertTrue(result.contains("<auth-user-pass>"))
    }

    @Test
    fun process_injectsVerbLevel() {
        val config = baseConfig().copy(logLevel = 7)

        val result = processor.process("client\nremote 1.2.3.4\nverb 3", config)

        assertFalse(result.contains("verb 3"))
        assertTrue(result.contains("verb 7"))
    }

    @Test
    fun process_doesNotInjectVerbWhenLogLevelNull() {
        val result = processor.process("client\nremote 1.2.3.4\nverb 2", baseConfig())

        assertTrue(result.contains("verb 2"))
    }

    @Test
    fun process_addsPersistHintsWhenAutoReconnectEnabled() {
        val config = baseConfig().copy(autoReconnect = true)

        val result = processor.process("client\nremote 1.2.3.4", config)

        assertTrue(result.contains("persist-tun"))
        assertTrue(result.contains("persist-key"))
    }

    @Test
    fun process_doesNotDuplicatePersistHints() {
        val config = baseConfig().copy(autoReconnect = true)

        val result = processor.process(
            "client\npersist-tun\nremote 1.2.3.4\npersist-key",
            config
        )

        assertEquals(1, result.lines().count { it.equals("persist-tun", ignoreCase = true) })
        assertEquals(1, result.lines().count { it.equals("persist-key", ignoreCase = true) })
    }

    @Test
    fun process_leavesConfigUntouchedWhenNoOverrides() {
        val input = "client\ndev tun\nremote 1.2.3.4"
        val result = processor.process(input, baseConfig())

        assertEquals(input + "\n", result)
    }

    private fun baseConfig(): SargoVpnConfig = SargoVpnConfig(
        vpnName = "Test",
        vpnConfig = "inline"
    )
}
