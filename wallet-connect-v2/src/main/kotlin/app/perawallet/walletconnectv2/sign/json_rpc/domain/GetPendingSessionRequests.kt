package app.perawallet.walletconnectv2.sign.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.sign.json_rpc.model.toRequest
import kotlinx.coroutines.supervisorScope

internal class GetPendingSessionRequests(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {

    suspend operator fun invoke(): List<Request<String>> = supervisorScope {
        jsonRpcHistory.getListOfPendingRecords()
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_REQUEST }
            .mapNotNull { record -> serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)?.toRequest(record) }
    }
}