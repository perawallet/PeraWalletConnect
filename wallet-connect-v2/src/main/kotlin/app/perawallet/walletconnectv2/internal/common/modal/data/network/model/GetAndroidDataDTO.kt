package app.perawallet.walletconnectv2.internal.common.modal.data.network.model

import com.squareup.moshi.Json

internal class GetAndroidDataDTO(
    @Json(name = "count")
    val count: Int,
    @Json(name = "data")
    val data: List<WalletDataDTO>,
)
