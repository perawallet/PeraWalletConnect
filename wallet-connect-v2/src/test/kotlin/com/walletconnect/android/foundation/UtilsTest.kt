package app.perawallet.walletconnectv2.foundation

import app.perawallet.walletconnectv2.foundation.util.generateClientToServerId
import app.perawallet.walletconnectv2.foundation.util.generateId
import junit.framework.TestCase.assertTrue
import org.junit.Test

class UtilsTest {

    @Test
    fun `generate client to client id test`() {
        val id = generateId()
        println(id)
        assertTrue(id > 0)
        assertTrue(id.toString().length == 16)
    }

    @Test
    fun `generate client to server id test`() {
        val id = generateClientToServerId()
        println(id)
        assertTrue(id > 0)
        assertTrue(id.toString().length == 19)
    }
}