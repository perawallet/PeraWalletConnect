@file:JvmSynthetic

package app.perawallet.walletconnectv2.internal.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionProposer(
    @Json(name = "publicKey")
    val publicKey: String,
    @Json(name = "metadata")
    val metadata: AppMetaData,
)