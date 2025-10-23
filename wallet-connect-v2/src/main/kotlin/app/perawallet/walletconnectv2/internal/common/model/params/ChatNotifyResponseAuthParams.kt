package app.perawallet.walletconnectv2.internal.common.model.params

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams

interface ChatNotifyResponseAuthParams {
    @JsonClass(generateAdapter = true)
    data class ResponseAuth(
        @Json(name = "responseAuth")
        val responseAuth: String,
    ) : ClientParams

    @JsonClass(generateAdapter = true)
    data class Auth(
        @Json(name = "auth")
        val auth: String,
    ) : ClientParams
}