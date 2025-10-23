package app.perawallet.walletconnectv2.foundation.common.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class SubscriptionId(val id: String)