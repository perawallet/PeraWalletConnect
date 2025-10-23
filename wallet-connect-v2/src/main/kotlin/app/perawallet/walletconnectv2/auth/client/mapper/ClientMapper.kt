

package app.perawallet.walletconnectv2.auth.client.mapper

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.model.ConnectionState
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Validation
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoType
import app.perawallet.walletconnectv2.verify.model.VerifyContext
import app.perawallet.walletconnectv2.auth.client.Auth
import app.perawallet.walletconnectv2.auth.common.model.AuthResponse
import app.perawallet.walletconnectv2.auth.common.model.Events
import app.perawallet.walletconnectv2.auth.common.model.PayloadParams
import app.perawallet.walletconnectv2.auth.common.model.PendingRequest
import app.perawallet.walletconnectv2.auth.common.model.Respond
import java.text.SimpleDateFormat
import java.util.*


internal fun Auth.Params.Respond.toCommon(): Respond = when (this) {
    is Auth.Params.Respond.Result -> Respond.Result(id, signature, issuer)
    is Auth.Params.Respond.Error -> Respond.Error(id, code, message)
}


internal fun ConnectionState.toClient(): Auth.Event.ConnectionStateChange =
    Auth.Event.ConnectionStateChange(Auth.Model.ConnectionState(this.isAvailable))


internal fun SDKError.toClient(): Auth.Event.Error = Auth.Event.Error(Auth.Model.Error(this.exception))


internal fun Events.OnAuthRequest.toClientAuthRequest(): Auth.Event.AuthRequest = Auth.Event.AuthRequest(id, pairingTopic, payloadParams.toClient())


internal fun Events.OnAuthRequest.toClientAuthContext(): Auth.Event.VerifyContext =
    Auth.Event.VerifyContext(id, verifyContext.origin, verifyContext.validation.toClientValidation(), verifyContext.verifyUrl, verifyContext.isScam)


internal fun Validation.toClientValidation(): Auth.Model.Validation =
    when (this) {
        Validation.VALID -> Auth.Model.Validation.VALID
        Validation.INVALID -> Auth.Model.Validation.INVALID
        Validation.UNKNOWN -> Auth.Model.Validation.UNKNOWN
    }

internal fun PayloadParams.toClient(): Auth.Model.PayloadParams =
    Auth.Model.PayloadParams(
        type = type,
        chainId = chainId,
        domain = domain,
        aud = aud,
        version = version,
        nonce = nonce,
        iat = iat,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
    )


internal fun Events.OnAuthResponse.toClient(): Auth.Event.AuthResponse = when (val response = response) {
    is AuthResponse.Error -> Auth.Event.AuthResponse(Auth.Model.Response.Error(id, response.code, response.message))
    is AuthResponse.Result -> Auth.Event.AuthResponse(Auth.Model.Response.Result(id, response.cacao.toClient()))
}


internal fun Auth.Params.Request.toCommon(): PayloadParams = PayloadParams(
    type = CacaoType.EIP4361.header,
    chainId = chainId,
    domain = domain,
    aud = aud,
    version = Cacao.Payload.CURRENT_VERSION,
    nonce = nonce,
    iat = SimpleDateFormat(Cacao.Payload.ISO_8601_PATTERN).format(Calendar.getInstance().time),
    nbf = nbf,
    exp = exp,
    statement = statement,
    requestId = requestId,
    resources = resources,
)


internal fun List<PendingRequest>.toClient(): List<Auth.Model.PendingRequest> =
    map { request ->
        Auth.Model.PendingRequest(
            request.id,
            request.pairingTopic,
            request.payloadParams.toClient()
        )
    }


internal fun Auth.Model.PayloadParams.toCommon(): PayloadParams =
    PayloadParams(
        type = type,
        chainId = chainId,
        domain = domain,
        aud = aud,
        version = version,
        nonce = nonce,
        iat = iat,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
    )


internal fun Auth.Model.Cacao.Signature.toCommon(): Cacao.Signature = Cacao.Signature(t, s, m)


internal fun Cacao.toClient(): Auth.Model.Cacao = Auth.Model.Cacao(header.toClient(), payload.toClient(), signature.toClient())


internal fun Cacao.Header.toClient(): Auth.Model.Cacao.Header = Auth.Model.Cacao.Header(t)


internal fun Cacao.Payload.toClient(): Auth.Model.Cacao.Payload =
    Auth.Model.Cacao.Payload(iss, domain, aud, version, nonce, iat, nbf, exp, statement, requestId, resources)


internal fun Cacao.Signature.toClient(): Auth.Model.Cacao.Signature = Auth.Model.Cacao.Signature(t, s, m)


internal fun VerifyContext.toClient(): Auth.Model.VerifyContext = Auth.Model.VerifyContext(id, origin, validation.toClientValidation(), verifyUrl, isScam)


internal fun Core.Model.Message.AuthRequest.toAuth(): Auth.Model.Message.AuthRequest = with(payloadParams) {
    Auth.Model.Message.AuthRequest(id, pairingTopic, metadata, Auth.Model.Message.AuthRequest.PayloadParams(type, chainId, domain, aud, version, nonce, iat, nbf, exp, statement, requestId, resources))
}