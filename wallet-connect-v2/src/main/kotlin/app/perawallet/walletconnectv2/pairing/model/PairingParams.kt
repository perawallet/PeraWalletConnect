package app.perawallet.walletconnectv2.pairing.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.utils.DefaultId

sealed class PairingParams : ClientParams {

    @JsonClass(generateAdapter = true)
    class DeleteParams(
        @Json(name = "code")
        val code: Int = Int.DefaultId,
        @Json(name = "message")
        val message: String,
    ) : PairingParams()

    @Suppress("CanSealedSubClassBeObject")
    class PingParams : PairingParams()
}