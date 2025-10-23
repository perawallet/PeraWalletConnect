package app.perawallet.walletconnectv2.foundation.common.model

import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.foundation.util.Empty

@JsonClass(generateAdapter = false)
data class Topic(val value: String = String.Empty)