

package app.perawallet.walletconnectv2.sign.client.mapper

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.model.ConnectionState
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Validation
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoType
import app.perawallet.walletconnectv2.internal.common.signing.cacao.RECAPS_STATEMENT
import app.perawallet.walletconnectv2.internal.common.signing.cacao.getStatement
import app.perawallet.walletconnectv2.utils.toClient
import app.perawallet.walletconnectv2.sign.client.Sign
import app.perawallet.walletconnectv2.sign.common.model.Request
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO


internal fun Sign.Model.JsonRpcResponse.toJsonRpcResponse(): JsonRpcResponse =
    when (this) {
        is Sign.Model.JsonRpcResponse.JsonRpcResult -> this.toRpcResult()
        is Sign.Model.JsonRpcResponse.JsonRpcError -> this.toRpcError()
    }


internal fun EngineDO.SettledSessionResponse.toClientSettledSessionResponse(): Sign.Model.SettledSessionResponse =
    when (this) {
        is EngineDO.SettledSessionResponse.Result -> Sign.Model.SettledSessionResponse.Result(settledSession.toClientActiveSession())
        is EngineDO.SettledSessionResponse.Error -> Sign.Model.SettledSessionResponse.Error(errorMessage)
    }


internal fun EngineDO.SessionAuthenticateResponse.toClientSessionAuthenticateResponse(): Sign.Model.SessionAuthenticateResponse =
    when (this) {
        is EngineDO.SessionAuthenticateResponse.Result -> Sign.Model.SessionAuthenticateResponse.Result(id, cacaos.toClient(), session?.toClientActiveSession())
        is EngineDO.SessionAuthenticateResponse.Error -> Sign.Model.SessionAuthenticateResponse.Error(id, code, message)
    }


internal fun EngineDO.SessionUpdateNamespacesResponse.toClientUpdateSessionNamespacesResponse(): Sign.Model.SessionUpdateResponse =
    when (this) {
        is EngineDO.SessionUpdateNamespacesResponse.Result -> Sign.Model.SessionUpdateResponse.Result(topic.value, namespaces.toMapOfClientNamespacesSession())
        is EngineDO.SessionUpdateNamespacesResponse.Error -> Sign.Model.SessionUpdateResponse.Error(errorMessage)
    }


internal fun EngineDO.JsonRpcResponse.toClientJsonRpcResponse(): Sign.Model.JsonRpcResponse =
    when (this) {
        is EngineDO.JsonRpcResponse.JsonRpcResult -> this.toClientJsonRpcResult()
        is EngineDO.JsonRpcResponse.JsonRpcError -> this.toClientJsonRpcError()
    }


internal fun EngineDO.SessionProposal.toClientSessionProposal(): Sign.Model.SessionProposal =
    Sign.Model.SessionProposal(
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toMapOfClientNamespacesProposal(),
        optionalNamespaces.toMapOfClientNamespacesProposal(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )


internal fun EngineDO.VerifyContext.toCore(): Sign.Model.VerifyContext =
    Sign.Model.VerifyContext(id, origin, this.validation.toClientValidation(), verifyUrl, isScam)

internal fun Validation.toClientValidation(): Sign.Model.Validation =
    when (this) {
        Validation.VALID -> Sign.Model.Validation.VALID
        Validation.INVALID -> Sign.Model.Validation.INVALID
        Validation.UNKNOWN -> Sign.Model.Validation.UNKNOWN
    }


internal fun EngineDO.SessionRequest.toClientSessionRequest(): Sign.Model.SessionRequest =
    Sign.Model.SessionRequest(
        topic = topic,
        chainId = chainId,
        peerMetaData = peerAppMetaData?.toClient(),
        request = Sign.Model.SessionRequest.JSONRPCRequest(
            id = request.id,
            method = request.method,
            params = request.params
        )
    )


internal fun Sign.Model.PayloadParams.toEngine(): EngineDO.PayloadParams = with(this) {
    EngineDO.PayloadParams(
        type = CacaoType.CAIP222.header,
        chains = chains,
        domain = domain,
        aud = aud,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        iat = iat,
        version = "1"
    )
}


internal fun Sign.Params.Authenticate.toAuthenticate(): EngineDO.Authenticate = with(this) {
    EngineDO.Authenticate(
        type = CacaoType.EIP4361.header,
        chains = chains,
        domain = domain,
        aud = uri,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        methods = methods,
        expiry = expiry
    )
}


internal fun Sign.Model.PayloadParams.toCacaoPayload(issuer: String): Sign.Model.Cacao.Payload = with(this) {
    Sign.Model.Cacao.Payload(issuer, domain, aud, "1", nonce, iat = iat, nbf, exp, getStatement(), requestId, resources)
}

private fun Sign.Model.PayloadParams.getStatement() =
    if (statement?.contains(RECAPS_STATEMENT) == true) {
        statement
    } else {
        Pair(statement, resources).getStatement()
    }


internal fun List<Sign.Model.Cacao>.toCommon(): List<Cacao> = this.map {
    with(it) {
        Cacao(
            Cacao.Header(header.t),
            Cacao.Payload(
                payload.iss,
                payload.domain,
                payload.aud,
                payload.version,
                payload.nonce,
                payload.iat,
                payload.nbf,
                payload.exp,
                payload.statement,
                payload.requestId,
                payload.resources
            ),
            Cacao.Signature(signature.t, signature.s, signature.m)
        )
    }
}


internal fun List<Cacao>.toClient(): List<Sign.Model.Cacao> = this.map {
    with(it) {
        Sign.Model.Cacao(
            Sign.Model.Cacao.Header(header.t),
            Sign.Model.Cacao.Payload(
                payload.iss,
                payload.domain,
                payload.aud,
                payload.version,
                payload.nonce,
                payload.iat,
                payload.nbf,
                payload.exp,
                payload.statement,
                payload.requestId,
                payload.resources
            ),
            Sign.Model.Cacao.Signature(signature.t, signature.s, signature.m)
        )
    }
}


internal fun Sign.Model.JsonRpcResponse.JsonRpcResult.toRpcResult(): JsonRpcResponse.JsonRpcResult = JsonRpcResponse.JsonRpcResult(id, result = result)


internal fun Sign.Model.JsonRpcResponse.JsonRpcError.toRpcError(): JsonRpcResponse.JsonRpcError = JsonRpcResponse.JsonRpcError(id, error = JsonRpcResponse.Error(code, message))


internal fun Sign.Model.SessionEvent.toEngineEvent(chainId: String): EngineDO.Event = EngineDO.Event(name, data, chainId)


internal fun EngineDO.SessionDelete.toClientDeletedSession(): Sign.Model.DeletedSession =
    Sign.Model.DeletedSession.Success(topic, reason)


internal fun EngineDO.SessionEvent.toClientSessionEvent(): Sign.Model.SessionEvent =
    Sign.Model.SessionEvent(name, data)


internal fun EngineDO.SessionAuthenticateEvent.toClientSessionAuthenticate(): Sign.Model.SessionAuthenticate = with(payloadParams) {
    Sign.Model.SessionAuthenticate(
        id,
        pairingTopic,
        participant.toClient(),
        payloadParams.toClient(),
        expiryTimestamp
    )
}


internal fun EngineDO.Participant.toClient(): Sign.Model.SessionAuthenticate.Participant = Sign.Model.SessionAuthenticate.Participant(publicKey, metadata.toClient())


internal fun EngineDO.PayloadParams.toClient(): Sign.Model.PayloadParams = Sign.Model.PayloadParams(
    type = type,
    chains = chains,
    domain = domain,
    aud = aud,
    nonce = nonce,
    nbf = nbf,
    exp = exp,
    statement = statement,
    requestId = requestId,
    resources = resources,
    iat = iat
)


internal fun EngineDO.SessionEvent.toClientEvent(): Sign.Model.Event =
    Sign.Model.Event(topic, name, data, chainId)


internal fun EngineDO.Session.toClientActiveSession(): Sign.Model.Session =
    Sign.Model.Session(
        pairingTopic,
        topic.value,
        expiry.seconds,
        requiredNamespaces.toMapOfClientNamespacesProposal(),
        optionalNamespaces?.toMapOfClientNamespacesProposal(),
        namespaces.toMapOfClientNamespacesSession(),
        peerAppMetaData?.toClient()
    )


internal fun EngineDO.SessionExtend.toClientActiveSession(): Sign.Model.Session =
    Sign.Model.Session(
        pairingTopic,
        topic.value,
        expiry.seconds,
        requiredNamespaces.toMapOfClientNamespacesProposal(),
        optionalNamespaces?.toMapOfClientNamespacesProposal(),
        namespaces.toMapOfClientNamespacesSession(),
        peerAppMetaData?.toClient()
    )


internal fun EngineDO.SessionRejected.toClientSessionRejected(): Sign.Model.RejectedSession =
    Sign.Model.RejectedSession(topic, reason)


internal fun EngineDO.SessionApproved.toClientSessionApproved(): Sign.Model.ApprovedSession =
    Sign.Model.ApprovedSession(topic, peerAppMetaData?.toClient(), namespaces.toMapOfClientNamespacesSession(), accounts)


internal fun Map<String, EngineDO.Namespace.Session>.toMapOfClientNamespacesSession(): Map<String, Sign.Model.Namespace.Session> =
    this.mapValues { (_, namespace) ->
        Sign.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Sign.Params.Request.toEngineDORequest(): EngineDO.Request =
    EngineDO.Request(sessionTopic, method, params, chainId, expiry?.let { Expiry(it) })


internal fun Sign.Params.Request.toSentRequest(requestId: Long): Sign.Model.SentRequest =
    Sign.Model.SentRequest(requestId, sessionTopic, method, params, chainId)


internal fun EngineDO.JsonRpcResponse.JsonRpcResult.toClientJsonRpcResult(): Sign.Model.JsonRpcResponse.JsonRpcResult =
    Sign.Model.JsonRpcResponse.JsonRpcResult(id, result)


internal fun EngineDO.SessionUpdateNamespaces.toClientSessionsNamespaces(): Sign.Model.UpdatedSession =
    Sign.Model.UpdatedSession(topic.value, namespaces.toMapOfClientNamespacesSession())


internal fun EngineDO.JsonRpcResponse.JsonRpcError.toClientJsonRpcError(): Sign.Model.JsonRpcResponse.JsonRpcError =
    Sign.Model.JsonRpcResponse.JsonRpcError(id, code = error.code, message = error.message)


internal fun EngineDO.PairingSettle.toClientSettledPairing(): Sign.Model.Pairing =
    Sign.Model.Pairing(topic.value, appMetaData?.toClient())


internal fun List<Request<String>>.mapToPendingRequests(): List<Sign.Model.PendingRequest> = map { request ->
    Sign.Model.PendingRequest(
        request.id,
        request.topic.value,
        request.method,
        request.chainId,
        request.params
    )
}


internal fun List<EngineDO.SessionRequest>.mapToPendingSessionRequests(): List<Sign.Model.SessionRequest> =
    map { request -> request.toClientSessionRequest() }


internal fun Request<SignParams.SessionAuthenticateParams>.toClient(): Sign.Model.SessionAuthenticate =
    Sign.Model.SessionAuthenticate(
        id = id,
        topic = topic.value,
        participant = Sign.Model.SessionAuthenticate.Participant(params.requester.publicKey, params.requester.metadata.toClient()),
        payloadParams = params.toClient(),
        expiry = params.expiryTimestamp
    )


internal fun SignParams.SessionAuthenticateParams.toClient(): Sign.Model.PayloadParams = with(this.authPayload) {
    Sign.Model.PayloadParams(
        type = type,
        chains = chains,
        domain = domain,
        aud = aud,
        nonce = nonce,
        nbf = nbf,
        exp = exp,
        statement = statement,
        requestId = requestId,
        resources = resources,
        iat = iat
    )
}


internal fun EngineDO.SessionPayloadResponse.toClientSessionPayloadResponse(): Sign.Model.SessionRequestResponse =
    Sign.Model.SessionRequestResponse(topic, chainId, method, result.toClientJsonRpcResponse())


internal fun Map<String, Sign.Model.Namespace.Proposal>.toMapOfEngineNamespacesRequired(): Map<String, EngineDO.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        EngineDO.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Proposal>.toMapOfEngineNamespacesOptional(): Map<String, EngineDO.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        EngineDO.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun Map<String, EngineDO.Namespace.Proposal>.toMapOfClientNamespacesProposal(): Map<String, Sign.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Session>.toMapOfEngineNamespacesSession(): Map<String, EngineDO.Namespace.Session> =
    mapValues { (_, namespace) ->
        EngineDO.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Proposal>.toProposalNamespacesVO(): Map<String, Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Namespace.Proposal(chains = namespace.chains, methods = namespace.methods, events = namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Session>.toSessionNamespacesVO(): Map<String, Namespace.Session> =
    mapValues { (_, namespace) ->
        Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Map<String, Namespace.Session>.toCore(): Map<String, Sign.Model.Namespace.Session> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Map<String, Core.Model.Namespace.Proposal>.toSign(): Map<String, Sign.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun ConnectionState.toClientConnectionState(): Sign.Model.ConnectionState =
    Sign.Model.ConnectionState(isAvailable)


internal fun EngineDO.ExpiredProposal.toClient(): Sign.Model.ExpiredProposal =
    Sign.Model.ExpiredProposal(pairingTopic, proposerPublicKey)


internal fun EngineDO.ExpiredRequest.toClient(): Sign.Model.ExpiredRequest =
    Sign.Model.ExpiredRequest(topic, id)


internal fun SDKError.toClientError(): Sign.Model.Error =
    Sign.Model.Error(this.exception)


internal fun Core.Model.Message.SessionProposal.toSign(): Sign.Model.Message.SessionProposal =
    Sign.Model.Message.SessionProposal(
        id,
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toSign(),
        optionalNamespaces.toSign(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )


internal fun Core.Model.Message.SessionAuthenticate.toSign(): Sign.Model.Message.SessionAuthenticate =
    Sign.Model.Message.SessionAuthenticate(
        id, topic, metadata, payloadParams.toClient(), expiry
    )

private fun Core.Model.Message.SessionAuthenticate.PayloadParams.toClient(): Sign.Model.PayloadParams {
    return with(this) {
        Sign.Model.PayloadParams(
            chains = chains,
            domain = domain,
            nonce = nonce,
            aud = aud,
            type = type,
            nbf = nbf,
            exp = exp,
            iat = iat,
            statement = requestId,
            resources = resources,
            requestId = requestId,
        )
    }
}


internal fun Core.Model.Message.SessionRequest.toSign(): Sign.Model.Message.SessionRequest =
    Sign.Model.Message.SessionRequest(
        topic,
        chainId,
        peerMetaData,
        Sign.Model.Message.SessionRequest.JSONRPCRequest(request.id, request.method, request.params)
    )