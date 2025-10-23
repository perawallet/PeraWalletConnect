package app.perawallet.walletconnectv2.sign.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.json_rpc.model.JsonRpcHistoryRecord
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.json_rpc.model.toRequest

internal class GetSessionRequestByIdUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {
    operator fun invoke(id: Long): Request<SignParams.SessionRequestParams>? {
        val record: JsonRpcHistoryRecord? = jsonRpcHistory.getRecordById(id)
        var entry: Request<SignParams.SessionRequestParams>? = null

        if (record != null) {
            val sessionRequest: SignRpc.SessionRequest? = serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)
            if (sessionRequest != null) {
                entry = record.toRequest(sessionRequest.params)
            }
        }

        return entry
    }
}