package app.perawallet.walletconnectv2.auth.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthRpc
import app.perawallet.walletconnectv2.auth.common.model.PendingRequest
import app.perawallet.walletconnectv2.auth.engine.mapper.toPendingRequest
import app.perawallet.walletconnectv2.auth.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.auth.json_rpc.model.toEntry
import app.perawallet.walletconnectv2.foundation.common.model.Topic

internal class GetPendingJsonRpcHistoryEntryByTopicUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {

    suspend operator fun invoke(topic: Topic): PendingRequest {
        return jsonRpcHistory.getListOfPendingRecords()
            .filter { record -> record.method == JsonRpcMethod.WC_AUTH_REQUEST && record.topic == topic.value }
            .mapNotNull { record -> serializer.tryDeserialize<AuthRpc.AuthRequest>(record.body)?.params?.toEntry(record) }
            .map { jsonRpcHistoryEntry -> jsonRpcHistoryEntry.toPendingRequest() }
            .first()
    }
}