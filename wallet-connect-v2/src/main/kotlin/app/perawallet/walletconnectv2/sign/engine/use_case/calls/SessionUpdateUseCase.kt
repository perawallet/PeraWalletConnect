package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.exception.CannotFindSequenceForTopic
import app.perawallet.walletconnectv2.internal.common.exception.GenericException
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.InvalidNamespaceException
import app.perawallet.walletconnectv2.sign.common.exceptions.NO_SEQUENCE_FOR_TOPIC_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.NotSettledSessionException
import app.perawallet.walletconnectv2.sign.common.exceptions.SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.UNAUTHORIZED_UPDATE_MESSAGE
import app.perawallet.walletconnectv2.sign.common.exceptions.UnauthorizedPeerException
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toMapOfNamespacesVOSession
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.supervisorScope

internal class SessionUpdateUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val logger: Logger,
) : SessionUpdateUseCaseInterface {

    override suspend fun sessionUpdate(
        topic: String,
        namespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    ) = supervisorScope {
        runCatching { validate(topic, namespaces) }.fold(
            onSuccess = {
                val params = SignParams.UpdateNamespacesParams(namespaces.toMapOfNamespacesVOSession())
                val sessionUpdate = SignRpc.SessionUpdate(params = params)
                val irnParams = IrnParams(Tags.SESSION_UPDATE, Ttl(dayInSeconds))

                try {
                    logger.log("Sending session update on topic: $topic")
                    sessionStorageRepository.insertTempNamespaces(topic, namespaces.toMapOfNamespacesVOSession(), sessionUpdate.id)
                    jsonRpcInteractor.publishJsonRpcRequest(
                        Topic(topic), irnParams, sessionUpdate,
                        onSuccess = {
                            logger.log("Update sent successfully, topic: $topic")
                            onSuccess()
                        },
                        onFailure = { error ->
                            logger.error("Sending session update error: $error, topic: $topic")
                            sessionStorageRepository.deleteTempNamespacesByRequestId(sessionUpdate.id)
                            onFailure(error)
                        })
                } catch (e: Exception) {
                    logger.error("Error updating namespaces: $e")
                    onFailure(GenericException("Error updating namespaces: $e"))
                }
            },
            onFailure = {
                logger.error("Error updating namespaces: $it")
                onFailure(it)
            }
        )
    }

    private fun validate(topic: String, namespaces: Map<String, EngineDO.Namespace.Session>) {
        if (!sessionStorageRepository.isSessionValid(Topic(topic))) {
            logger.error("Sending session update error: cannot find sequence for topic: $topic")
            throw CannotFindSequenceForTopic("$NO_SEQUENCE_FOR_TOPIC_MESSAGE$topic")
        }

        val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(Topic(topic))

        if (!session.isSelfController) {
            logger.error("Sending session update error: unauthorized peer")
            throw UnauthorizedPeerException(UNAUTHORIZED_UPDATE_MESSAGE)
        }

        if (!session.isAcknowledged) {
            logger.error("Sending session update error: session is not acknowledged")
            throw NotSettledSessionException("$SESSION_IS_NOT_ACKNOWLEDGED_MESSAGE$topic")
        }

        SignValidator.validateSessionNamespace(namespaces.toMapOfNamespacesVOSession(), session.requiredNamespaces) { error ->
            logger.error("Sending session update error: invalid namespaces $error")
            throw InvalidNamespaceException(error.message)
        }
    }
}

internal interface SessionUpdateUseCaseInterface {
    suspend fun sessionUpdate(
        topic: String,
        namespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit,
    )
}