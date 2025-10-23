package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.InvalidEventException
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.UNAUTHORIZED_EMIT_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.UnauthorizedEventException
import app.perawallet.walletconnectv2.sign.common.exceptions.UnauthorizedPeerException
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.payload.SessionEventVO
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import app.perawallet.walletconnectv2.foundation.util.generateId
import kotlinx.coroutines.supervisorScope

internal class EmitEventUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
) : EmitEventUseCaseInterface {

    override suspend fun emit(topic: String, event: EngineDO.Event, id: Long?, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        runCatching { validate(topic, event) }.fold(
            onSuccess = {
                val eventParams = SignParams.EventParams(SessionEventVO(event.name, event.data), event.chainId)
                val sessionEvent = SignRpc.SessionEvent(id = id ?: generateId(), params = eventParams)
                val irnParams = IrnParams(Tags.SESSION_EVENT, Ttl(fiveMinutesInSeconds), true)

                logger.log("Emitting event on topic: $topic")
                jsonRpcInteractor.publishJsonRpcRequest(Topic(topic), irnParams, sessionEvent,
                    onSuccess = {
                        logger.log("Event sent successfully, on topic: $topic")
                        onSuccess()
                    },
                    onFailure = { error ->
                        logger.error("Sending event error: $error, on topic: $topic")
                        onFailure(error)
                    }
                )
            },
            onFailure = { error ->
                logger.error("Sending event error: $error, on topic: $topic")
                onFailure(error)
            }
        )
    }

    private fun validate(topic: String, event: EngineDO.Event) {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Emit - cannot find sequence for topic: $topic")
            throw CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic")
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))
        if (!session.isSelfController) {
            logger.error("Emit - unauthorized peer: $topic")
            throw UnauthorizedPeerException(UNAUTHORIZED_EMIT_MESSAGE)
        }

        SignValidator.validateEvent(event) { error ->
            logger.error("Emit - invalid event: $topic")
            throw InvalidEventException(error.message)
        }

        val namespaces = session.sessionNamespaces
        SignValidator.validateChainIdWithEventAuthorisation(event.chainId, event.name, namespaces) { error ->
            logger.error("Emit - unauthorized event: $topic")
            throw UnauthorizedEventException(error.message)
        }
    }
}

internal interface EmitEventUseCaseInterface {
    suspend fun emit(topic: String, event: EngineDO.Event, id: Long? = null, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}