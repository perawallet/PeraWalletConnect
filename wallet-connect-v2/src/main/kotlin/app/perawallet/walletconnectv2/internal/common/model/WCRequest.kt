package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.internal.utils.Empty

data class WCRequest(
    val topic: Topic,
    val id: Long,
    val method: String,
    val params: ClientParams,
    val message: String = String.Empty,
    val publishedAt: Long = 0,
    val encryptedMessage: String = String.Empty,
    val attestation: String? = null,
    val transportType: TransportType
)