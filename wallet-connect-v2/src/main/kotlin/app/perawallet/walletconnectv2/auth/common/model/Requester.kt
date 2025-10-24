

package app.perawallet.walletconnectv2.auth.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData

@JsonClass(generateAdapter = true)
internal data class Requester(
    @Json(name = "publicKey")
    val publicKey: String,
    @Json(name = "metadata")
    val metadata: AppMetaData
)