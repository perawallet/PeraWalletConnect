package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.internal.common.exception.Uncategorized
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.PeerError
import app.perawallet.walletconnectv2.sign.common.model.type.Sequences
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDOEvent
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toPeerError
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionEventUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(request: WCRequest, params: SignParams.EventParams) = supervisorScope {
        logger.log("Session event received on topic: ${request.topic}")
        val irnParams = IrnParams(Tags.SESSION_EVENT_RESPONSE, Ttl(fiveMinutesInSeconds))
        try {
            SignValidator.validateEvent(params.toEngineDOEvent()) { error ->
                logger.error("Session event received failure on topic: ${request.topic} - $error")
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            if (!sessionStorageRepository.isSessionValid(request.topic)) {
                logger.error("Session event received failure on topic: ${request.topic} - invalid session")
                jsonRpcInteractor.respondWithError(request, Uncategorized.NoMatchingTopic(Sequences.SESSION.name, request.topic.value), irnParams)
                return@supervisorScope
            }

            val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(request.topic)
            if (!session.isPeerController) {
                logger.error("Session event received failure on topic: ${request.topic} - unauthorized peer")
                jsonRpcInteractor.respondWithError(request, PeerError.Unauthorized.Event(Sequences.SESSION.name), irnParams)
                return@supervisorScope
            }
            if (!session.isAcknowledged) {
                logger.error("Session event received failure on topic: ${request.topic} - no matching topic")
                jsonRpcInteractor.respondWithError(request, Uncategorized.NoMatchingTopic(Sequences.SESSION.name, request.topic.value), irnParams)
                return@supervisorScope
            }

            val event = params.event
            SignValidator.validateChainIdWithEventAuthorisation(params.chainId, event.name, session.sessionNamespaces) { error ->
                logger.error("Session event received failure on topic: ${request.topic} - $error")
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            jsonRpcInteractor.respondWithSuccess(request, irnParams)
            logger.log("Session event received on topic: ${request.topic} - emitting")
            _events.emit(params.toEngineDO(request.topic))
        } catch (e: Exception) {
            logger.error("Session event received failure on topic: ${request.topic} - $e")
            jsonRpcInteractor.respondWithError(request, Uncategorized.GenericError("Cannot emit an event: ${e.message}, topic: ${request.topic}"), irnParams)
            _events.emit(SDKError(e))
            return@supervisorScope
        }
    }
}