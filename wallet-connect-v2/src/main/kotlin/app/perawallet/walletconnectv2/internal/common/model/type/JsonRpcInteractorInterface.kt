package app.perawallet.walletconnectv2.internal.common.model.type

import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.WCResponse
import kotlinx.coroutines.flow.SharedFlow

interface JsonRpcInteractorInterface {
    val clientSyncJsonRpc: SharedFlow<WCRequest>
    val peerResponse: SharedFlow<WCResponse>
    val internalErrors: SharedFlow<SDKError>
}