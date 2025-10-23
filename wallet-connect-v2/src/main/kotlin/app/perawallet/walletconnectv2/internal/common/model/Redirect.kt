package app.perawallet.walletconnectv2.internal.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Redirect(
    @Json(name = "native")
    val native: String? = null,
    @Json(name = "universal")
    val universal: String? = null,
    @Json(name = "linkMode")
    val linkMode: Boolean = false
)