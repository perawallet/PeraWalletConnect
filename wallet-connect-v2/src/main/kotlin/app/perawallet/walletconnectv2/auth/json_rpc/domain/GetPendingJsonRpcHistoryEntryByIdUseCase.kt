package app.perawallet.walletconnectv2.auth.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.json_rpc.model.JsonRpcHistoryRecord
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthRpc
import app.perawallet.walletconnectv2.auth.common.model.JsonRpcHistoryEntry
import app.perawallet.walletconnectv2.auth.json_rpc.model.toEntry

internal class GetPendingJsonRpcHistoryEntryByIdUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer
) {

    operator fun invoke(id: Long): JsonRpcHistoryEntry? {
        val record: JsonRpcHistoryRecord? = jsonRpcHistory.getPendingRecordById(id)
        var entry: JsonRpcHistoryEntry? = null

        if (record != null) {
            val authRequest: AuthRpc.AuthRequest? = serializer.tryDeserialize<AuthRpc.AuthRequest>(record.body)
            if (authRequest != null) {
                entry = record.toEntry(authRequest.params)
            }
        }

        return entry
    }
}