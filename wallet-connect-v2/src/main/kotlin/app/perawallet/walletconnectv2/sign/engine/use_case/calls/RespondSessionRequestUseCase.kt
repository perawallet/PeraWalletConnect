package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.exception.RequestExpiredException
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Direction
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.engine.sessionRequestEventsQueue
import app.perawallet.walletconnectv2.sign.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RespondSessionRequestUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val getPendingJsonRpcHistoryEntryByIdUseCase: GetPendingJsonRpcHistoryEntryByIdUseCase,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val logger: Logger,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
) : RespondSessionRequestUseCaseInterface {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    override suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        val topicWrapper = Topic(topic)
        if (!sessionStorageRepository.isSessionValid(topicWrapper)) {
            logger.error("Request response -  invalid session: $topic, id: ${jsonRpcResponse.id}")
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }
        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(topicWrapper)
            .run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }

        if (getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id) == null) {
            logger.error("Request doesn't exist: $topic, id: ${jsonRpcResponse.id}")
            throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
        }
        getPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcResponse.id)?.params?.request?.expiryTimestamp?.let {
            if (Expiry(it).isExpired()) {
                logger.error("Request Expired: $topic, id: ${jsonRpcResponse.id}")
                throw RequestExpiredException("This request has expired, id: ${jsonRpcResponse.id}")
            }
        }

        if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
            if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
            try {
                removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                linkModeJsonRpcInteractor.triggerResponse(Topic(topic), jsonRpcResponse, session.peerAppLink)
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE_RESPONSE.id.toString(),
                        Properties(correlationId = jsonRpcResponse.id, clientId = clientId, direction = Direction.SENT.state)
                    )
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        } else {
            val irnParams = IrnParams(Tags.SESSION_REQUEST_RESPONSE, Ttl(fiveMinutesInSeconds))
            logger.log("Sending session request response on topic: $topic, id: ${jsonRpcResponse.id}")
            jsonRpcInteractor.publishJsonRpcResponse(topic = Topic(topic), params = irnParams, response = jsonRpcResponse,
                onSuccess = {
                    onSuccess()
                    logger.log("Session request response sent successfully on topic: $topic, id: ${jsonRpcResponse.id}")
                    scope.launch {
                        supervisorScope {
                            removePendingSessionRequestAndEmit(jsonRpcResponse.id)
                        }
                    }
                },
                onFailure = { error ->
                    logger.error("Sending session response error: $error, id: ${jsonRpcResponse.id}")
                    onFailure(error)
                }
            )
        }
    }

    private suspend fun removePendingSessionRequestAndEmit(id: Long) {
        verifyContextStorageRepository.delete(id)
        sessionRequestEventsQueue.find { pendingRequestEvent -> pendingRequestEvent.request.request.id == id }?.let { event ->
            sessionRequestEventsQueue.remove(event)
        }
        if (sessionRequestEventsQueue.isNotEmpty()) {
            sessionRequestEventsQueue.find { event -> if (event.request.expiry != null) !event.request.expiry.isExpired() else true }?.let { event ->
                _events.emit(event)
            }
        }
    }
}

internal interface RespondSessionRequestUseCaseInterface {
    val events: SharedFlow<EngineEvent>
    suspend fun respondSessionRequest(
        topic: String,
        jsonRpcResponse: JsonRpcResponse,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}