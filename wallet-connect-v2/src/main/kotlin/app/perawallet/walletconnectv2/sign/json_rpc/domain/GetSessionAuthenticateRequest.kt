package app.perawallet.walletconnectv2.sign.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.json_rpc.model.JsonRpcHistoryRecord
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.json_rpc.model.toRequest

internal class GetSessionAuthenticateRequest(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {
    internal operator fun invoke(id: Long): Request<SignParams.SessionAuthenticateParams>? {
        val record: JsonRpcHistoryRecord? = jsonRpcHistory.getRecordById(id)
        var entry: Request<SignParams.SessionAuthenticateParams>? = null

        if (record != null) {
            val authRequest: SignRpc.SessionAuthenticate? = serializer.tryDeserialize<SignRpc.SessionAuthenticate>(record.body)
            if (authRequest != null) {
                entry = record.toRequest(authRequest.params)
            }
        }

        return entry
    }
}