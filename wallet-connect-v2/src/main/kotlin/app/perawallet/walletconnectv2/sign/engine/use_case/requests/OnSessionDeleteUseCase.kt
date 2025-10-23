package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.Uncategorized
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
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionDeleteUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val crypto: KeyManagementRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(request: WCRequest, params: SignParams.DeleteParams) = supervisorScope {
        logger.log("Session delete received on topic: ${request.topic}")
        val irnParams = IrnParams(Tags.SESSION_DELETE_RESPONSE, Ttl(dayInSeconds))
        try {
            if (!sessionStorageRepository.isSessionValid(request.topic)) {
                logger.error("Session delete received failure on topic: ${request.topic} - invalid session")
                jsonRpcInteractor.respondWithError(request, Uncategorized.NoMatchingTopic(Sequences.SESSION.name, request.topic.value), irnParams)
                return@supervisorScope
            }
            jsonRpcInteractor.unsubscribe(request.topic,
                onSuccess = {
                    logger.log("Session delete received on topic: ${request.topic} - unsubscribe success")
                    try {
                        crypto.removeKeys(request.topic.value)
                    } catch (e: Exception) {
                        logger.error("Remove keys exception:$e")
                    }
                },
                onFailure = { error -> logger.error("Session delete received on topic: ${request.topic} - unsubscribe error $error") })
            sessionStorageRepository.deleteSession(request.topic)
            logger.log("Session delete received on topic: ${request.topic} - emitting")
            _events.emit(params.toEngineDO(request.topic))
        } catch (e: Exception) {
            logger.error("Session delete received failure on topic: ${request.topic} - $e")
            jsonRpcInteractor.respondWithError(request, Uncategorized.GenericError("Cannot delete a session: ${e.message}, topic: ${request.topic}"), irnParams)
            _events.emit(SDKError(e))
            return@supervisorScope
        }
    }
}