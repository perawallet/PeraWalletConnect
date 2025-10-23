package app.perawallet.walletconnectv2.auth.common.json_rpc

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.auth.common.model.PayloadParams
import app.perawallet.walletconnectv2.auth.common.model.Requester

internal sealed class AuthParams : ClientParams {

    @JsonClass(generateAdapter = true)
    internal data class RequestParams(
        @Json(name = "requester")
        val requester: Requester,
        @Json(name = "payloadParams")
        val payloadParams: PayloadParams,
        @Json(name = "expiry")
        val expiry: Expiry? = null
    ) : AuthParams()
}