package app.perawallet.walletconnectv2.auth.engine.mapper

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Issuer
import app.perawallet.walletconnectv2.internal.common.signing.cacao.toCAIP222Message
import app.perawallet.walletconnectv2.auth.common.model.JsonRpcHistoryEntry
import app.perawallet.walletconnectv2.auth.common.model.PayloadParams
import app.perawallet.walletconnectv2.auth.common.model.PendingRequest

@JvmSynthetic
internal fun PayloadParams.toCacaoPayload(iss: Issuer): Cacao.Payload = Cacao.Payload(
    iss.value,
    domain = domain,
    aud = aud,
    version = version,
    nonce = nonce,
    iat = iat,
    nbf = nbf,
    exp = exp,
    statement = statement,
    requestId = requestId,
    resources = resources
)

@JvmSynthetic
internal fun PayloadParams.toCAIP222Message(iss: Issuer, chainName: String = "Ethereum"): String =
    this.toCacaoPayload(iss).toCAIP222Message(chainName)

@JvmSynthetic
internal fun JsonRpcHistoryEntry.toPendingRequest(): PendingRequest = PendingRequest(id, topic.value, params.payloadParams)