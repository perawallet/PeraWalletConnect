package app.perawallet.walletconnectv2.sign.engine.use_case.responses

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.WCResponse
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionSettleResponseUseCase(
    private val sessionStorageRepository: SessionStorageRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val crypto: KeyManagementRepository,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse) = supervisorScope {
        try {
            logger.log("Session settle response received on topic: ${wcResponse.topic}")
            val sessionTopic = wcResponse.topic
            if (!sessionStorageRepository.isSessionValid(sessionTopic)) {
                logger.error("Peer failed to settle session: invalid session")
                return@supervisorScope
            }
            val session = sessionStorageRepository.getSessionWithoutMetadataByTopic(sessionTopic).run {
                val peerAppMetaData = metadataStorageRepository.getByTopicAndType(this.topic, AppMetaDataType.PEER)
                this.copy(selfAppMetaData = selfAppMetaData, peerAppMetaData = peerAppMetaData)
            }

            when (wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> {
                    logger.log("Session settle success received")
                    sessionStorageRepository.acknowledgeSession(sessionTopic)
                    _events.emit(EngineDO.SettledSessionResponse.Result(session.toEngineDO()))
                }

                is JsonRpcResponse.JsonRpcError -> {
                    logger.error("Peer failed to settle session: ${wcResponse.response.errorMessage}")
                    jsonRpcInteractor.unsubscribe(sessionTopic, onSuccess = {
                        runCatching {
                            sessionStorageRepository.deleteSession(sessionTopic)
                            crypto.removeKeys(sessionTopic.value)
                        }.onFailure { logger.error(it) }
                    })
                }
            }
        } catch (e: Exception) {
            logger.error("Peer failed to settle session: $e")
            _events.emit(SDKError(e))
        }
    }
}