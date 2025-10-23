package app.perawallet.walletconnectv2.internal.common.json_rpc.model

import app.perawallet.walletconnectv2.internal.common.model.TransportType

data class JsonRpcHistoryRecord(
    val id: Long,
    val topic: String,
    val method: String,
    val body: String,
    val response: String?,
    val transportType: TransportType?
)
