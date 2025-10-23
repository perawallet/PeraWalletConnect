package app.perawallet.walletconnectv2.internal

import app.perawallet.walletconnectv2.internal.utils.compareDomains
import org.junit.Test

class URLComparisonTest {

    @Test
    fun compareUrlsTest1() {
        val result = compareDomains(metadataUrl = "https://www.known-url.com/", originUrl = "https://www.known-url.com")
        assert(result)
    }

    @Test
    fun compareUrlsTest2() {
        val result = compareDomains(metadataUrl = "https://www.known-url.com/", originUrl = "https://www.known-url.com/subdomain/subdomain2")
        assert(result)
    }

    @Test
    fun compareUrlsTest3() {
        val result = compareDomains(metadataUrl = "https://www.known-url.com/subdomain3", originUrl = "https://www.known-url.com/subdomain/subdomain2")
        assert(result)
    }

    @Test
    fun compareUrlsTest4() {
        val result = compareDomains(metadataUrl = "http://www.known-url.com", originUrl = "https://www.known-url.com/")
        assert(result)
    }

    @Test
    fun compareUrlsTest5() {
        val result = compareDomains(metadataUrl = "https://known-url.com", originUrl = "https://www.known-url.com/")
        assert(result)
    }

    @Test
    fun compareUrlsTest6() {
        val result = compareDomains(metadataUrl = "https://www.known-url.com", originUrl = "https://known-url.com/")
        assert(result)
    }
}