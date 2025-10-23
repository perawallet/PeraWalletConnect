package app.perawallet.walletconnectv2.internal.common.model.params

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams

sealed interface CoreChatParams : ClientParams {

    @JsonClass(generateAdapter = true)
    data class ReceiptParams(
        @Json(name = "receiptAuth")
        val receiptAuth: String,
    ) : CoreChatParams
}