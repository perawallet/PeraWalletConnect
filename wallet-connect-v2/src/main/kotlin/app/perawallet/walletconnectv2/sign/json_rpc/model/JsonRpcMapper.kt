

package app.perawallet.walletconnectv2.sign.json_rpc.model

import app.perawallet.walletconnectv2.internal.common.json_rpc.model.JsonRpcHistoryRecord
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams

@JvmSynthetic
internal fun SignRpc.SessionRequest.toRequest(entry: JsonRpcHistoryRecord): Request<String> =
    Request(
        entry.id,
        Topic(entry.topic),
        params.request.method,
        params.chainId,
        params.request.params,
        if (params.request.expiryTimestamp != null) Expiry(params.request.expiryTimestamp) else null,
        transportType = entry.transportType
    )

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toRequest(params: SignParams.SessionRequestParams): Request<SignParams.SessionRequestParams> =
    Request(
        id,
        Topic(topic),
        method,
        params.chainId,
        params,
        transportType = transportType
    )

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toRequest(params: SignParams.SessionAuthenticateParams): Request<SignParams.SessionAuthenticateParams> =
    Request(
        id,
        Topic(topic),
        method,
        null,
        params,
        Expiry(params.expiryTimestamp),
        transportType = transportType
    )

@JvmSynthetic
internal fun SignRpc.SessionAuthenticate.toRequest(entry: JsonRpcHistoryRecord): Request<SignParams.SessionAuthenticateParams> =
    Request(
        entry.id,
        Topic(entry.topic),
        entry.method,
        null,
        params,
        Expiry(params.expiryTimestamp),
        transportType = entry.transportType
    )