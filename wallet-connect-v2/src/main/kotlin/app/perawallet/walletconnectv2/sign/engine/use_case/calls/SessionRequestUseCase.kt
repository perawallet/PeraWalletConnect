package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.exception.InvalidExpiryException
import app.perawallet.walletconnectv2.internal.common.json_rpc.domain.link_mode.LinkModeJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Namespace
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.internal.utils.currentTimeInSeconds
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Direction
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.InvalidRequestException
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.UnauthorizedMethodException
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.payload.SessionRequestVO
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

internal class SessionRequestUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val linkModeJsonRpcInteractor: LinkModeJsonRpcInteractorInterface,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val insertEventUseCase: InsertEventUseCase,
    private val clientId: String,
    private val logger: Logger,
) : SessionRequestUseCaseInterface {
    private val _errors: MutableSharedFlow<SDKError> = MutableSharedFlow()
    override val errors: SharedFlow<SDKError> = _errors.asSharedFlow()

    override suspend fun sessionRequest(request: EngineDO.Request, onSuccess: (Long) -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(request.topic))) {
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE${request.topic}"))
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(request.topic))
            .run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(peerAppMetaData = peerAppMetaData)
            }

        val nowInSeconds = currentTimeInSeconds
        if (!CoreValidator.isExpiryWithinBounds(request.expiry)) {
            logger.error("Sending session request error: expiry not within bounds")
            return@supervisorScope onFailure(InvalidExpiryException())
        }
        val expiry = request.expiry ?: Expiry(currentTimeInSeconds + fiveMinutesInSeconds)
        SignValidator.validateSessionRequest(request) { error ->
            logger.error("Sending session request error: invalid session request, ${error.message}")
            return@supervisorScope onFailure(InvalidRequestException(error.message))
        }

        val namespaces: Map<String, Namespace.Session> = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(request.topic)).sessionNamespaces
        SignValidator.validateChainIdWithMethodAuthorisation(request.chainId, request.method, namespaces) { error ->
            logger.error("Sending session request error: unauthorized method, ${error.message}")
            return@supervisorScope onFailure(UnauthorizedMethodException(error.message))
        }

        val params = SignParams.SessionRequestParams(SessionRequestVO(request.method, request.params, expiry.seconds), request.chainId)
        val sessionPayload = SignRpc.SessionRequest(params = params)

        if (session.transportType == TransportType.LINK_MODE && session.peerLinkMode == true) {
            if (session.peerAppLink.isNullOrEmpty()) return@supervisorScope onFailure(IllegalStateException("App link is missing"))
            try {
                linkModeJsonRpcInteractor.triggerRequest(sessionPayload, Topic(request.topic), session.peerAppLink)
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE.id.toString(),
                        Properties(correlationId = sessionPayload.id, clientId = clientId, direction = Direction.SENT.state)
                    )
                )
            } catch (e: Exception) {
                onFailure(e)
            }
        } else {
            val irnParamsTtl = expiry.run {
                val defaultTtl = fiveMinutesInSeconds
                val extractedTtl = seconds - nowInSeconds
                val newTtl = extractedTtl.takeIf { extractedTtl >= defaultTtl } ?: defaultTtl

                Ttl(newTtl)
            }
            val irnParams = IrnParams(Tags.SESSION_REQUEST, irnParamsTtl, true)
            val requestTtlInSeconds = expiry.run { seconds - nowInSeconds }

            logger.log("Sending session request on topic: ${request.topic}}")
            jsonRpcInteractor.publishJsonRpcRequest(Topic(request.topic), irnParams, sessionPayload,
                onSuccess = {
                    logger.log("Session request sent successfully on topic: ${request.topic}")
                    onSuccess(sessionPayload.id)
                    scope.launch {
                        try {
                            withTimeout(TimeUnit.SECONDS.toMillis(requestTtlInSeconds)) {
                                collectResponse(sessionPayload.id) { cancel() }
                            }
                        } catch (e: TimeoutCancellationException) {
                            _errors.emit(SDKError(e))
                        }
                    }
                },
                onFailure = { error ->
                    logger.error("Sending session request error: $error")
                    onFailure(error)
                }
            )
        }
    }

    private suspend fun collectResponse(id: Long, onResponse: (Result<JsonRpcResponse.JsonRpcResult>) -> Unit = {}) {
        jsonRpcInteractor.peerResponse
            .filter { response -> response.response.id == id }
            .collect { response ->
                when (val result = response.response) {
                    is JsonRpcResponse.JsonRpcResult -> onResponse(Result.success(result))
                    is JsonRpcResponse.JsonRpcError -> onResponse(Result.failure(Throwable(result.errorMessage)))
                }
            }
    }
}

internal interface SessionRequestUseCaseInterface {
    val errors: SharedFlow<SDKError>
    suspend fun sessionRequest(request: EngineDO.Request, onSuccess: (Long) -> Unit, onFailure: (Throwable) -> Unit)
}