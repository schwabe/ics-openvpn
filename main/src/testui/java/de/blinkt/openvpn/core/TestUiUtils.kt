import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.fragments.Utils
import org.junit.Assert
import org.junit.Test

class TestUiUtils {
    @Test
    fun testCompatModeUtil() {
        Assert.assertEquals(2, Utils.mapCompatVer(20400))
        Assert.assertEquals(0, Utils.mapCompatVer(20707))
        Assert.assertEquals(3, Utils.mapCompatVer(11723))
    }

    @Test
    fun testWarnings() {
        val vp = VpnProfile("unittest")
        vp.mUseCustomConfig = true;
        vp.mCustomConfigOptions = "\ntls-cipher DEFAULT:@SECLEVEL=0\n"

        val warnings = mutableListOf<String>()
        Utils.addSoftWarnings(warnings, vp)
        Assert.assertTrue(warnings.size >= 1)

    }
}