package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.internal.common.exception.Uncategorized
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.model.type.Sequences
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDOSessionExtend
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toPeerError
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionExtendUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(request: WCRequest, requestParams: SignParams.ExtendParams) = supervisorScope {
        val irnParams = IrnParams(Tags.SESSION_EXTEND_RESPONSE, Ttl(dayInSeconds))
        logger.log("Session extend received on topic: ${request.topic}")
        try {
            if (!sessionStorageRepository.isSessionValid(request.topic)) {
                logger.error("Session extend received failure on topic: ${request.topic}")
                jsonRpcInteractor.respondWithError(request, Uncategorized.NoMatchingTopic(Sequences.SESSION.name, request.topic.value), irnParams)
                return@supervisorScope
            }

            val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(request.topic)
            val newExpiry = requestParams.expiry
            SignValidator.validateSessionExtend(newExpiry, session.expiry.seconds) { error ->
                logger.error("Session extend received failure on topic: ${request.topic} - invalid request: $error")
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            sessionStorageRepository.extendSession(request.topic, newExpiry)
            jsonRpcInteractor.respondWithSuccess(request, irnParams)
            logger.log("Session extend received on topic: ${request.topic} - emitting")
            _events.emit(session.toEngineDOSessionExtend(Expiry(newExpiry)))
        } catch (e: Exception) {
            logger.error("Session extend received failure on topic: ${request.topic}: $e")
            jsonRpcInteractor.respondWithError(request, Uncategorized.GenericError("Cannot update a session: ${e.message}, topic: ${request.topic}"), irnParams)
            _events.emit(SDKError(e))
            return@supervisorScope
        }
    }
}