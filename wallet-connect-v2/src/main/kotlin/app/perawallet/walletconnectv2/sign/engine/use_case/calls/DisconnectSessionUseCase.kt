package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.exception.Reason
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.supervisorScope

internal class DisconnectSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
) : DisconnectSessionUseCaseInterface {
    override suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Sending session disconnect error: invalid session $topic")
            return@supervisorScope onFailure(CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic"))
        }

        val deleteParams = SignParams.DeleteParams(Reason.UserDisconnected.code, Reason.UserDisconnected.message)
        val sessionDelete = SignRpc.SessionDelete(params = deleteParams)
        val irnParams = IrnParams(Tags.SESSION_DELETE, Ttl(dayInSeconds))

        logger.log("Sending session disconnect on topic: $topic")
        jsonRpcInteractor.publishJsonRpcRequest(Topic(topic), irnParams, sessionDelete,
            onSuccess = {
                logger.log("Disconnect sent successfully on topic: $topic")
                sessionStorageRepository.deleteSession(Topic(topic))
                jsonRpcInteractor.unsubscribe(Topic(topic))
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Sending session disconnect error: $error on topic: $topic")
                onFailure(error)
            }
        )
    }
}

internal interface DisconnectSessionUseCaseInterface {
    suspend fun disconnect(topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}