package app.perawallet.walletconnectv2.sign.common.model

import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.foundation.common.model.Topic

internal data class Request<T>(
    val id: Long,
    val topic: Topic,
    val method: String,
    val chainId: String?,
    val params: T,
    val expiry: Expiry? = null,
    val transportType: TransportType?
)