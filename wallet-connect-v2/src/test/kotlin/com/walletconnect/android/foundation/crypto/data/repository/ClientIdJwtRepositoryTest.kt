package app.perawallet.walletconnectv2.foundation.crypto.data.repository

import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.util.jwt.jwtIatAndExp
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test

internal class ClientIdJwtRepositoryTest {
    private val sut = spyk(object : BaseClientIdJwtRepository() {

        override fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {}

        override fun getKeyPair(): Pair<String, String> {
            return "884ab67f787b69e534bfdba8d5beb4e719700e90ac06317ed177d49e5a33be5a" to "58e0254c211b858ef7896b00e3f36beeb13d568d47c6031c4218b87718061295"
        }
    })
    private val serverUrl = "wss://relay.walletconnect.org"

    // Expected JWT for given nonce
    private val expectedJWT =
        "eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6a2V5Ono2TWtvZEhad25lVlJTaHRhTGY4SktZa3hwREdwMXZHWm5wR21kQnBYOE0yZXh4SCIsInN1YiI6ImM0NzlmZTVkYzQ2NGU3NzFlNzhiMTkzZDIzOWE2NWI1OGQyNzhjYWQxYzM0YmZiMGI1NzE2ZTViYjUxNDkyOGUiLCJhdWQiOiJ3c3M6Ly9yZWxheS53YWxsZXRjb25uZWN0Lm9yZyIsImlhdCI6MTY1NjkxMDA5NywiZXhwIjoxNjU2OTk2NDk3fQ.lV9WBz1j3uQvDlv8NFnzymGczKQMWFsSqaZdXs4cUwFepP5LVC2eYIHdGsYkiJfwJxNia3LghX20GBW09SuwBA"

    @Before
    fun setUp() {
        every { sut.generateSubject() } returns "c479fe5dc464e771e78b193d239a65b58d278cad1c34bfb0b5716e5bb514928e"
        mockkStatic(::jwtIatAndExp)
        every { jwtIatAndExp(any(), any(), any(), any()) } returns (1656910097L to (1656910097L + 86400L))
    }

    @Test
    fun generateJWTTest() {
        val actualJWT = sut.generateJWT(serverUrl)
        assertEquals(expectedJWT, actualJWT)
    }
}