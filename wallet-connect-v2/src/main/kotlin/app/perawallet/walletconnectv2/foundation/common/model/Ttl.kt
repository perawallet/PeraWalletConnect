package app.perawallet.walletconnectv2.foundation.common.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class Ttl(val seconds: Long)