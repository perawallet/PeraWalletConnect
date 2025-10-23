package app.perawallet.walletconnectv2.internal.common.model.type

interface JsonRpcClientSync<T : ClientParams> : SerializableJsonRpc {
    val id: Long
    val method: String
    val jsonrpc: String
    val params: T
}