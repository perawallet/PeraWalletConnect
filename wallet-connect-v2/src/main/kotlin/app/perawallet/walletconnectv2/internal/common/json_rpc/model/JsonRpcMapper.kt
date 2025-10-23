@file:JvmSynthetic

package app.perawallet.walletconnectv2.internal.common.json_rpc.model

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.relay.Subscription
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.WCResponse
import app.perawallet.walletconnectv2.internal.common.model.sync.ClientJsonRpc
import app.perawallet.walletconnectv2.internal.common.model.type.ClientParams
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.network.model.Relay

@JvmSynthetic
internal fun JsonRpcHistoryRecord.toWCResponse(result: JsonRpcResponse, params: ClientParams): WCResponse =
    WCResponse(Topic(topic), method, result, params)

@JvmSynthetic
internal fun IrnParams.toRelay(): Relay.Model.IrnParams =
    Relay.Model.IrnParams(tag.id, ttl.seconds, prompt)

internal fun Subscription.toWCRequest(clientJsonRpc: ClientJsonRpc, params: ClientParams, transportType: TransportType): WCRequest =
    WCRequest(topic, clientJsonRpc.id, clientJsonRpc.method, params, decryptedMessage, publishedAt, encryptedMessage, attestation, transportType)