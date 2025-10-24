package app.perawallet.walletconnectv2.web3.wallet.client

import app.perawallet.walletconnectv2.auth.client.Auth
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoType
import app.perawallet.walletconnectv2.sign.client.Sign


internal fun Map<String, Wallet.Model.Namespace.Session>.toSign(): Map<String, Sign.Model.Namespace.Session> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Session>.toWallet(): Map<String, Wallet.Model.Namespace.Session> =
    mapValues { (_, namespace) ->
        Wallet.Model.Namespace.Session(namespace.chains, namespace.accounts, namespace.methods, namespace.events)
    }


internal fun Map<String, Sign.Model.Namespace.Proposal>.toWalletProposalNamespaces(): Map<String, Wallet.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Wallet.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun Map<String, Wallet.Model.Namespace.Proposal>.toSignProposalNamespaces(): Map<String, Sign.Model.Namespace.Proposal> =
    mapValues { (_, namespace) ->
        Sign.Model.Namespace.Proposal(namespace.chains, namespace.methods, namespace.events)
    }


internal fun Wallet.Model.JsonRpcResponse.toSign(): Sign.Model.JsonRpcResponse =
    when (this) {
        is Wallet.Model.JsonRpcResponse.JsonRpcResult -> this.toSign()
        is Wallet.Model.JsonRpcResponse.JsonRpcError -> this.toSign()
    }


internal fun Wallet.Model.JsonRpcResponse.JsonRpcResult.toSign(): Sign.Model.JsonRpcResponse.JsonRpcResult =
    Sign.Model.JsonRpcResponse.JsonRpcResult(id, result)


internal fun Wallet.Model.JsonRpcResponse.JsonRpcError.toSign(): Sign.Model.JsonRpcResponse.JsonRpcError =
    Sign.Model.JsonRpcResponse.JsonRpcError(id, code, message)


internal fun Wallet.Params.AuthRequestResponse.toAuth(): Auth.Params.Respond = when (this) {
    is Wallet.Params.AuthRequestResponse.Result -> Auth.Params.Respond.Result(id, signature.toAuth(), issuer)
    is Wallet.Params.AuthRequestResponse.Error -> Auth.Params.Respond.Error(id, code, message)
}


internal fun Wallet.Model.Cacao.Signature.toAuth(): Auth.Model.Cacao.Signature = Auth.Model.Cacao.Signature(t, s, m)


internal fun Wallet.Model.Cacao.Signature.toSign(): Sign.Model.Cacao.Signature = Sign.Model.Cacao.Signature(t, s, m)


internal fun Wallet.Model.SessionEvent.toSign(): Sign.Model.SessionEvent = Sign.Model.SessionEvent(name, data)


internal fun Wallet.Model.Event.toSign(): Sign.Model.Event = Sign.Model.Event(topic, name, data, chainId)


internal fun Wallet.Model.PayloadParams.toAuth(): Auth.Model.PayloadParams =
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


internal fun Wallet.Model.PayloadAuthRequestParams.toSign(): Sign.Model.PayloadParams =
    Sign.Model.PayloadParams(
        type = type ?: CacaoType.CAIP222.header,
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


internal fun Sign.Model.Session.toWallet(): Wallet.Model.Session = Wallet.Model.Session(
    pairingTopic, topic, expiry, requiredNamespaces.toWalletProposalNamespaces(), optionalNamespaces?.toWalletProposalNamespaces(), namespaces.toWallet(), metaData
)


internal fun List<Sign.Model.PendingRequest>.mapToPendingRequests(): List<Wallet.Model.PendingSessionRequest> = map { request ->
    Wallet.Model.PendingSessionRequest(
        request.requestId,
        request.topic,
        request.method,
        request.chainId,
        request.params
    )
}


internal fun List<Sign.Model.SessionRequest>.mapToPendingSessionRequests(): List<Wallet.Model.SessionRequest> = map { request ->
    Wallet.Model.SessionRequest(
        request.topic,
        request.chainId,
        request.peerMetaData,
        Wallet.Model.SessionRequest.JSONRPCRequest(request.request.id, request.request.method, request.request.params)
    )
}

internal fun Auth.Model.PayloadParams.toWallet(): Wallet.Model.PayloadParams =
    Wallet.Model.PayloadParams(
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


internal fun List<Auth.Model.PendingRequest>.toWallet(): List<Wallet.Model.PendingAuthRequest> =
    map { request ->
        Wallet.Model.PendingAuthRequest(
            request.id,
            request.pairingTopic,
            request.payloadParams.toWallet()
        )
    }


internal fun Sign.Model.SessionProposal.toWallet(): Wallet.Model.SessionProposal =
    Wallet.Model.SessionProposal(
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toWalletProposalNamespaces(),
        optionalNamespaces.toWalletProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )


internal fun Sign.Model.SessionAuthenticate.toWallet(): Wallet.Model.SessionAuthenticate =
    Wallet.Model.SessionAuthenticate(id, topic, participant.toWallet(), payloadParams.toWallet())


internal fun Sign.Model.SessionAuthenticate.Participant.toWallet(): Wallet.Model.SessionAuthenticate.Participant = Wallet.Model.SessionAuthenticate.Participant(publicKey, metadata)


internal fun Sign.Model.PayloadParams.toWallet(): Wallet.Model.PayloadAuthRequestParams =
    Wallet.Model.PayloadAuthRequestParams(
        chains = chains,
        type = type ?: CacaoType.CAIP222.header,
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

internal fun Sign.Model.VerifyContext.toWallet(): Wallet.Model.VerifyContext =
    Wallet.Model.VerifyContext(id, origin, this.validation.toWallet(), verifyUrl, isScam)

internal fun Sign.Model.Validation.toWallet(): Wallet.Model.Validation =
    when (this) {
        Sign.Model.Validation.VALID -> Wallet.Model.Validation.VALID
        Sign.Model.Validation.INVALID -> Wallet.Model.Validation.INVALID
        Sign.Model.Validation.UNKNOWN -> Wallet.Model.Validation.UNKNOWN
    }

internal fun Auth.Model.Validation.toWallet(): Wallet.Model.Validation =
    when (this) {
        Auth.Model.Validation.VALID -> Wallet.Model.Validation.VALID
        Auth.Model.Validation.INVALID -> Wallet.Model.Validation.INVALID
        Auth.Model.Validation.UNKNOWN -> Wallet.Model.Validation.UNKNOWN
    }


internal fun Sign.Model.SessionRequest.toWallet(): Wallet.Model.SessionRequest =
    Wallet.Model.SessionRequest(
        topic = topic,
        chainId = chainId,
        peerMetaData = peerMetaData,
        request = Wallet.Model.SessionRequest.JSONRPCRequest(
            id = request.id,
            method = request.method,
            params = request.params
        )
    )


internal fun Sign.Model.DeletedSession.toWallet(): Wallet.Model.SessionDelete =
    when (this) {
        is Sign.Model.DeletedSession.Success -> Wallet.Model.SessionDelete.Success(topic, reason)
        is Sign.Model.DeletedSession.Error -> Wallet.Model.SessionDelete.Error(error)
    }


internal fun Sign.Model.SettledSessionResponse.toWallet(): Wallet.Model.SettledSessionResponse =
    when (this) {
        is Sign.Model.SettledSessionResponse.Result -> Wallet.Model.SettledSessionResponse.Result(session.toWallet())
        is Sign.Model.SettledSessionResponse.Error -> Wallet.Model.SettledSessionResponse.Error(errorMessage)
    }


internal fun Sign.Model.SessionUpdateResponse.toWallet(): Wallet.Model.SessionUpdateResponse =
    when (this) {
        is Sign.Model.SessionUpdateResponse.Result -> Wallet.Model.SessionUpdateResponse.Result(topic, namespaces.toWallet())
        is Sign.Model.SessionUpdateResponse.Error -> Wallet.Model.SessionUpdateResponse.Error(errorMessage)
    }


internal fun Sign.Model.ExpiredProposal.toWallet(): Wallet.Model.ExpiredProposal = Wallet.Model.ExpiredProposal(pairingTopic, proposerPublicKey)


internal fun Sign.Model.ExpiredRequest.toWallet(): Wallet.Model.ExpiredRequest = Wallet.Model.ExpiredRequest(topic, id)


internal fun Auth.Event.AuthRequest.toWallet(): Wallet.Model.AuthRequest = Wallet.Model.AuthRequest(id, pairingTopic, payloadParams.toWallet())


internal fun Auth.Event.VerifyContext.toWallet(): Wallet.Model.VerifyContext = Wallet.Model.VerifyContext(id, origin, this.validation.toWallet(), verifyUrl, isScam)


internal fun Auth.Model.VerifyContext.toWallet(): Wallet.Model.VerifyContext = Wallet.Model.VerifyContext(id, origin, this.validation.toWallet(), verifyUrl, isScam)


internal fun Wallet.Model.SessionProposal.toSign(): Sign.Model.SessionProposal =
    Sign.Model.SessionProposal(
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toSignProposalNamespaces(),
        optionalNamespaces.toSignProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )


internal fun Sign.Model.Message.SessionProposal.toWallet(): Wallet.Model.Message.SessionProposal =
    Wallet.Model.Message.SessionProposal(
        id,
        pairingTopic,
        name,
        description,
        url,
        icons,
        redirect,
        requiredNamespaces.toWalletProposalNamespaces(),
        optionalNamespaces.toWalletProposalNamespaces(),
        properties,
        proposerPublicKey,
        relayProtocol,
        relayData
    )


internal fun Sign.Model.Message.SessionRequest.toWallet(): Wallet.Model.Message.SessionRequest =
    Wallet.Model.Message.SessionRequest(
        topic,
        chainId,
        peerMetaData,
        Wallet.Model.Message.SessionRequest.JSONRPCRequest(request.id, request.method, request.params)
    )


internal fun Auth.Model.Message.AuthRequest.toWallet(): Wallet.Model.Message.AuthRequest = with(payloadParams) {
    Wallet.Model.Message.AuthRequest(
        id,
        pairingTopic,
        metadata,
        Wallet.Model.Message.AuthRequest.PayloadParams(type, chainId, domain, aud, version, nonce, iat, nbf, exp, statement, requestId, resources)
    )
}


internal fun List<Wallet.Model.Cacao>.toSign(): List<Sign.Model.Cacao> = mutableListOf<Sign.Model.Cacao>().apply {
    this@toSign.forEach { cacao: Wallet.Model.Cacao ->
        with(cacao) {
            add(
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
            )
        }
    }
}


internal fun Sign.Model.Cacao.toWallet(): Wallet.Model.Cacao = with(this) {
    Wallet.Model.Cacao(
        Wallet.Model.Cacao.Header(header.t),
        Wallet.Model.Cacao.Payload(
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
        Wallet.Model.Cacao.Signature(signature.t, signature.s, signature.m)
    )
}


internal fun Sign.Model.ConnectionState.Reason.toWallet(): Wallet.Model.ConnectionState.Reason = when (this) {
	is Sign.Model.ConnectionState.Reason.ConnectionClosed -> Wallet.Model.ConnectionState.Reason.ConnectionClosed(this.message)
	is Sign.Model.ConnectionState.Reason.ConnectionFailed -> Wallet.Model.ConnectionState.Reason.ConnectionFailed(this.throwable)
}