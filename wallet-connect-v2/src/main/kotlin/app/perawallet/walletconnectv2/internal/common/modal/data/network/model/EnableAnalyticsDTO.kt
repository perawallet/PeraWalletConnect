package app.perawallet.walletconnectv2.internal.common.modal.data.network.model

import com.squareup.moshi.Json

data class EnableAnalyticsDTO(
    @Json(name = "isAnalyticsEnabled")
    val isAnalyticsEnabled: Boolean
)
