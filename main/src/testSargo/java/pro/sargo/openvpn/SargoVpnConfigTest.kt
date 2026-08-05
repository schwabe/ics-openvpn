package pro.sargo.openvpn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SargoVpnConfigTest {

    @Test
    fun fromPreferences_parsesAllFields() {
        val config = SargoVpnConfig.fromPreferences(
            vpnName = "Office VPN",
            vpnConfig = "content://pro.sargo.launcher/config/office.ovpn",
            connect = "1",
            alwaysOn = "1",
            remove = "Old VPN,Another VPN",
            removeAll = "0"
        )

        assertNotNull(config)
        assertEquals("Office VPN", config?.vpnName)
        assertEquals("content://pro.sargo.launcher/config/office.ovpn", config?.vpnConfig)
        assertTrue(config?.connect == true)
        assertTrue(config?.alwaysOn == true)
        assertEquals("Old VPN,Another VPN", config?.remove)
        assertFalse(config?.removeAll == true)
    }

    @Test
    fun fromPreferences_defaultsAreApplied() {
        val config = SargoVpnConfig.fromPreferences(
            vpnName = "Minimal",
            vpnConfig = "inline config",
            connect = null,
            alwaysOn = null,
            remove = null,
            removeAll = null
        )

        assertNotNull(config)
        assertEquals("Minimal", config?.vpnName)
        assertEquals("inline config", config?.vpnConfig)
        assertFalse(config?.connect == true)
        assertNull(config?.alwaysOn)
        assertEquals("", config?.remove)
        assertFalse(config?.removeAll == true)
    }

    @Test
    fun fromPreferences_trimsWhitespace() {
        val config = SargoVpnConfig.fromPreferences(
            vpnName = "  Trimmed  ",
            vpnConfig = "  content://test  ",
            connect = " 1 ",
            alwaysOn = " 0 ",
            remove = "  A , B ",
            removeAll = " 1 "
        )

        assertNotNull(config)
        assertEquals("Trimmed", config?.vpnName)
        assertEquals("content://test", config?.vpnConfig)
        assertTrue(config?.connect == true)
        assertFalse(config?.alwaysOn == true)
        assertEquals("A , B", config?.remove)
        assertTrue(config?.removeAll == true)
    }

    @Test
    fun fromPreferences_rejectsMissingName() {
        val config = SargoVpnConfig.fromPreferences(
            vpnName = null,
            vpnConfig = "inline config",
            connect = "1",
            alwaysOn = "1",
            remove = "",
            removeAll = "0"
        )
        assertNull(config)
    }

    @Test
    fun fromPreferences_rejectsMissingConfig() {
        val config = SargoVpnConfig.fromPreferences(
            vpnName = "Name",
            vpnConfig = "",
            connect = "1",
            alwaysOn = "1",
            remove = "",
            removeAll = "0"
        )
        assertNull(config)
    }

    @Test
    fun fromPreferences_alwaysOnValues() {
        assertTrue(SargoVpnConfig.fromPreferences("A", "c", "0", "1", "", "0")?.alwaysOn == true)
        assertFalse(SargoVpnConfig.fromPreferences("A", "c", "0", "0", "", "0")?.alwaysOn == true)
        assertNull(SargoVpnConfig.fromPreferences("A", "c", "0", "", "", "0")?.alwaysOn)
        assertNull(SargoVpnConfig.fromPreferences("A", "c", "0", "yes", "", "0")?.alwaysOn)
    }

    @Test
    fun fromPreferences_booleanParsing() {
        assertTrue(SargoVpnConfig.fromPreferences("A", "c", "1", null, "", "")?.connect == true)
        assertFalse(SargoVpnConfig.fromPreferences("A", "c", "0", null, "", "")?.connect == true)
        assertFalse(SargoVpnConfig.fromPreferences("A", "c", "yes", null, "", "")?.connect == true)
        assertTrue(SargoVpnConfig.fromPreferences("A", "c", "", "", "", "1")?.removeAll == true)
        assertFalse(SargoVpnConfig.fromPreferences("A", "c", "", "", "", "0")?.removeAll == true)
    }
}
