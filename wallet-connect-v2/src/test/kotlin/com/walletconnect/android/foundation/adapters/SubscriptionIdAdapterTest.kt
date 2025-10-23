package app.perawallet.walletconnectv2.foundation.adapters

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import app.perawallet.walletconnectv2.foundation.common.adapters.SubscriptionIdAdapter
import app.perawallet.walletconnectv2.foundation.common.model.SubscriptionId
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

internal class SubscriptionIdAdapterTest {
    private val moshi = Moshi.Builder()
        .addLast { _, _, _ ->
            SubscriptionIdAdapter
        }
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun fromJson() {
        val expectedSubscriptionId = "subscriptionId1"
        val expected = SubscriptionId(expectedSubscriptionId)

        val resultSubscriptionId = moshi.adapter(SubscriptionId::class.java).fromJson(expectedSubscriptionId)

        assertNotNull(resultSubscriptionId)
        assertEquals(expected, resultSubscriptionId)
    }
}