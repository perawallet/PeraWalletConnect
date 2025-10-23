@file:JvmSynthetic

package app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.payload

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class SessionEventVO(
    @Json(name = "name")
    val name: String,
    @Json(name = "data")
    val data: Any
)