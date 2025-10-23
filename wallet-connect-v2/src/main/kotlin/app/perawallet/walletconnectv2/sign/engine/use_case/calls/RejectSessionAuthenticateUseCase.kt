package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.RequestExpiredException
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.EnvelopeType
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Participants
import app.perawallet.walletconnectv2.internal.common.model.SymmetricKey
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Direction
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.MissingSessionAuthenticateRequest
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.json_rpc.domain.GetPendingSessionAuthenticateRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RejectSessionAuthenticateUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val getPendingSessionAuthenticateRequest: GetPendingSessionAuthenticateRequest,
    private val crypto: KeyManagementRepository,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
    private val logger: Logger
) : RejectSessionAuthenticateUseCaseInterface {
    override suspend fun rejectSessionAuthenticate(id: Long, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        val jsonRpcHistoryEntry = getPendingSessionAuthenticateRequest(id)
        if (jsonRpcHistoryEntry == null) {
            logger.error(MissingSessionAuthenticateRequest().message)
            onFailure(MissingSessionAuthenticateRequest())
            return@supervisorScope
        }

        jsonRpcHistoryEntry.expiry?.let {
            if (it.isExpired()) {
                logger.error("Session Authenticate Request Expired: ${jsonRpcHistoryEntry.topic}, id: ${jsonRpcHistoryEntry.id}")
                throw RequestExpiredException("This request has expired, id: ${jsonRpcHistoryEntry.id}")
            }
        }

        //todo: handle error codes
        val response = JsonRpcResponse.JsonRpcError(id, error = JsonRpcResponse.Error(12001, reason))
        val sessionAuthenticateParams: SignParams.SessionAuthenticateParams = jsonRpcHistoryEntry.params
        val receiverMetadata: AppMetaData = sessionAuthenticateParams.requester.metadata
        val receiverPublicKey = PublicKey(sessionAuthenticateParams.requester.publicKey)
        val senderPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
        val symmetricKey: SymmetricKey = crypto.generateSymmetricKeyFromKeyAgreement(senderPublicKey, receiverPublicKey)
        val responseTopic: Topic = crypto.getTopicFromKey(receiverPublicKey)
        crypto.setKey(symmetricKey, responseTopic.value)

        if (jsonRpcHistoryEntry.transportType == TransportType.LINK_MODE && receiverMetadata.redirect?.linkMode == true) {
            if (receiverMetadata.redirect.universal.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
            try {
                linkModeJsonRpcInteractor.triggerResponse(
                    responseTopic,
                    response,
                    receiverMetadata.redirect?.universal!!,
                    Participants(senderPublicKey, receiverPublicKey),
                    EnvelopeType.ONE
                )
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_AUTHENTICATE_LINK_MODE_RESPONSE_REJECT.id.toString(),
                        Properties(clientId = clientId, correlationId = id, direction = Direction.SENT.state)
                    )
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        } else {
            val irnParams = IrnParams(Tags.SESSION_AUTHENTICATE_RESPONSE_REJECT, Ttl(dayInSeconds), false)
            logger.log("Sending Session Authenticate Reject on topic: $responseTopic")
            jsonRpcInteractor.publishJsonRpcResponse(
                responseTopic, irnParams, response, envelopeType = EnvelopeType.ONE, participants = Participants(senderPublicKey, receiverPublicKey),
                onSuccess = {
                    logger.log("Session Authenticate Reject Responded on topic: $responseTopic")
                    scope.launch { supervisorScope { verifyContextStorageRepository.delete(id) } }
                    onSuccess()
                },
                onFailure = { error ->
                    logger.error("Session Authenticate Error Responded on topic: $responseTopic")
                    scope.launch { supervisorScope { verifyContextStorageRepository.delete(id) } }
                    onFailure(error)
                }
            )
        }
    }
}

internal interface RejectSessionAuthenticateUseCaseInterface {
    suspend fun rejectSessionAuthenticate(id: Long, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}