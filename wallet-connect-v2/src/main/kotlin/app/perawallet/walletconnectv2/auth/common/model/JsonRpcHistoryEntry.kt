package app.perawallet.walletconnectv2.auth.common.model

import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.foundation.common.model.Topic

internal data class JsonRpcHistoryEntry(
    val id: Long,
    val topic: Topic,
    val method: String,
    val params: AuthParams.RequestParams,
    val response: String?
)