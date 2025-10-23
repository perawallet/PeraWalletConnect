package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.Invalid
import app.perawallet.walletconnectv2.internal.common.exception.InvalidExpiryException
import app.perawallet.walletconnectv2.internal.common.model.EnvelopeType
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Participants
import app.perawallet.walletconnectv2.internal.common.model.SymmetricKey
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.params.CoreAuthParams
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoType
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoVerifier
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Issuer
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.auth.client.mapper.toCommon
import app.perawallet.walletconnectv2.auth.common.exceptions.InvalidCacaoException
import app.perawallet.walletconnectv2.auth.common.exceptions.MissingAuthRequestException
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.model.Respond
import app.perawallet.walletconnectv2.auth.engine.mapper.toCacaoPayload
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import app.perawallet.walletconnectv2.auth.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RespondAuthRequestUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val getPendingJsonRpcHistoryEntryByIdUseCase: GetPendingJsonRpcHistoryEntryByIdUseCase,
    private val crypto: KeyManagementRepository,
    private val cacaoVerifier: CacaoVerifier,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val logger: Logger,
    private val pairingController: PairingControllerInterface
) : RespondAuthRequestUseCaseInterface {

    override suspend fun respond(respond: Respond, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        val jsonRpcHistoryEntry = getPendingJsonRpcHistoryEntryByIdUseCase(respond.id)

        if (jsonRpcHistoryEntry == null) {
            logger.error(MissingAuthRequestException.message)
            onFailure(MissingAuthRequestException)
            return@supervisorScope
        }

        val authParams: AuthParams.RequestParams = jsonRpcHistoryEntry.params
        val response: JsonRpcResponse = handleResponse(respond, authParams)

        val receiverPublicKey = PublicKey(authParams.requester.publicKey)
        val senderPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
        val symmetricKey: SymmetricKey = crypto.generateSymmetricKeyFromKeyAgreement(senderPublicKey, receiverPublicKey)
        val responseTopic: Topic = crypto.getTopicFromKey(receiverPublicKey)

        authParams.expiry?.let { expiry ->
            if (checkExpiry(expiry, responseTopic, respond, authParams)) return@supervisorScope onFailure(InvalidExpiryException())
        }

        crypto.setKey(symmetricKey, responseTopic.value)
        val irnParams = IrnParams(Tags.AUTH_REQUEST_RESPONSE, Ttl(dayInSeconds), false)
        jsonRpcInteractor.publishJsonRpcResponse(
            responseTopic, irnParams, response, envelopeType = EnvelopeType.ONE, participants = Participants(senderPublicKey, receiverPublicKey),
            onSuccess = {
                logger.log("Success Responded on topic: $responseTopic")
                scope.launch {
                    supervisorScope {
                        verifyContextStorageRepository.delete(respond.id)
                    }
                }
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Error Responded on topic: $responseTopic")
                scope.launch {
                    supervisorScope {
                        verifyContextStorageRepository.delete(respond.id)
                    }
                }
                onFailure(error)
            }
        )
    }

    private fun checkExpiry(expiry: Expiry, responseTopic: Topic, respond: Respond, authParams: AuthParams.RequestParams): Boolean {
        if (expiry.isExpired()) {
                val irnParams = IrnParams(Tags.AUTH_REQUEST_RESPONSE, Ttl(dayInSeconds))
                val wcRequest = WCRequest(responseTopic, respond.id, JsonRpcMethod.WC_AUTH_REQUEST, authParams, transportType = TransportType.RELAY)
                jsonRpcInteractor.respondWithError(wcRequest, Invalid.RequestExpired, irnParams)
                return true
            }
            return false
    }

    private fun handleResponse(respond: Respond, authParams: AuthParams.RequestParams) = when (respond) {
        is Respond.Error -> JsonRpcResponse.JsonRpcError(respond.id, error = JsonRpcResponse.Error(respond.code, respond.message))
        is Respond.Result -> {
            val issuer = Issuer(respond.iss)
            val payload: Cacao.Payload = authParams.payloadParams.toCacaoPayload(issuer)
            val cacao = Cacao(CacaoType.EIP4361.toHeader(), payload, respond.signature.toCommon())
            val responseParams = CoreAuthParams.ResponseParams(cacao.header, cacao.payload, cacao.signature)
            if (!cacaoVerifier.verify(cacao)) throw InvalidCacaoException
            JsonRpcResponse.JsonRpcResult(respond.id, result = responseParams)
        }
    }
}

internal interface RespondAuthRequestUseCaseInterface {
    suspend fun respond(respond: Respond, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}