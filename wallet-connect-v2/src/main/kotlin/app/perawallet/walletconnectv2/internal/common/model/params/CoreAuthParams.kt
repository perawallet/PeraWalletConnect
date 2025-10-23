package app.perawallet.walletconnectv2.internal.common.model.params

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao

open class CoreAuthParams : ClientParams {
    @JsonClass(generateAdapter = true)
    data class ResponseParams(
        @Json(name = "h")
        val header: Cacao.Header,
        @Json(name = "p")
        val payload: Cacao.Payload,
        @Json(name = "s")
        val signature: Cacao.Signature,
    ) : CoreAuthParams()
}