package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.internal.utils.weekInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.NotSettledSessionException
import app.perawallet.walletconnectv2.sign.common.exceptions.SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.supervisorScope

internal class ExtendSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
) : ExtendSessionUseCaseInterface {

    override suspend fun extend(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))
        if (!session.isAcknowledged) {
            logger.error("Sending session extend error: not acknowledged session on topic: $topic")
            return@supervisorScope onFailure(NotSettledSessionException("$SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE$topic"))
        }

        val newExpiration = session.expiry.seconds + weekInSeconds
        sessionStorageRepository.extendSession(Topic(topic), newExpiration)
        val sessionExtend = SignRpc.SessionExtend(params = SignParams.ExtendParams(newExpiration))
        val irnParams = IrnParams(Tags.SESSION_EXTEND, Ttl(dayInSeconds))

        logger.log("Sending session extend on topic: $topic")
        jsonRpcInteractor.publishJsonRpcRequest(
            Topic(topic), irnParams, sessionExtend,
            onSuccess = {
                logger.log("Session extend sent successfully on topic: $topic")
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Sending session extend error: $error on topic: $topic")
                onFailure(error)
            })
    }
}

internal interface ExtendSessionUseCaseInterface {
    suspend fun extend(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}