package app.perawallet.walletconnectv2.push.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PushBody(
    @Json(name = "client_id")
    val clientId: String,
    @Json(name = "token")
    val token: String,
    @Json(name = "type")
    val type: String = "fcm",
    @Json(name = "always_raw")
    val enableEncrypted: Boolean?
)