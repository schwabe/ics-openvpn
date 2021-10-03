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
}