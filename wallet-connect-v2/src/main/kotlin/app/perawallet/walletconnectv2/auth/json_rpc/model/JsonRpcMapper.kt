package app.perawallet.walletconnectv2.auth.json_rpc.model

import app.perawallet.walletconnectv2.internal.common.json_rpc.model.JsonRpcHistoryRecord
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.model.JsonRpcHistoryEntry
import app.perawallet.walletconnectv2.foundation.common.model.Topic


internal fun JsonRpcHistoryRecord.toEntry(params: AuthParams.RequestParams): JsonRpcHistoryEntry =
    JsonRpcHistoryEntry(
        id,
        Topic(topic),
        method,
        params,
        response
    )


internal fun AuthParams.RequestParams.toEntry(record: JsonRpcHistoryRecord): JsonRpcHistoryEntry =
    JsonRpcHistoryEntry(
        record.id,
        Topic(record.topic),
        record.method,
        this,
        record.response
    )