package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.sign.json_rpc.model.toRequest

internal class GetPendingAuthenticateRequestUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) : GetPendingAuthenticateRequestUseCaseInterface {
    override suspend fun getPendingAuthenticateRequests(): List<Request<SignParams.SessionAuthenticateParams>> {
        return jsonRpcHistory.getListOfPendingRecords()
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_AUTHENTICATE }
            .mapNotNull { record -> serializer.tryDeserialize<SignRpc.SessionAuthenticate>(record.body)?.toRequest(record) }
    }
}

internal interface GetPendingAuthenticateRequestUseCaseInterface {
    suspend fun getPendingAuthenticateRequests(): List<Request<SignParams.SessionAuthenticateParams>>
}