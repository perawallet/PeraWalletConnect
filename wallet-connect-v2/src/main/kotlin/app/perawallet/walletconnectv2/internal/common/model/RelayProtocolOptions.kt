@file:JvmSynthetic

package app.perawallet.walletconnectv2.internal.common.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RelayProtocolOptions(
    val protocol: String = "irn",
    val data: String? = null
)