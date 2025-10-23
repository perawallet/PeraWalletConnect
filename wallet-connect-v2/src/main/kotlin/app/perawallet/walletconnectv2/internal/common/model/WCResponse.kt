package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.foundation.common.model.Topic

data class WCResponse(
    val topic: Topic,
    val method: String,
    val response: JsonRpcResponse,
    val params: ClientParams,
)