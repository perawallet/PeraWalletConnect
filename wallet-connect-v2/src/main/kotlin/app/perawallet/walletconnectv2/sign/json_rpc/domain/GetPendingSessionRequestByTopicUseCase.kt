package app.perawallet.walletconnectv2.sign.json_rpc.domain

import app.perawallet.walletconnectv2.internal.common.json_rpc.data.JsonRpcSerializer
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.common.storage.rpc.JsonRpcHistory
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionRequest
import app.perawallet.walletconnectv2.sign.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.sign.json_rpc.model.toRequest
import kotlinx.coroutines.supervisorScope

internal class GetPendingSessionRequestByTopicUseCase(
    private val jsonRpcHistory: JsonRpcHistory,
    private val serializer: JsonRpcSerializer,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
) : GetPendingSessionRequestByTopicUseCaseInterface {

    override suspend fun getPendingSessionRequests(topic: Topic): List<EngineDO.SessionRequest> = supervisorScope {
        jsonRpcHistory.getListOfPendingRecordsByTopic(topic)
            .filter { record -> record.method == JsonRpcMethod.WC_SESSION_REQUEST }
            .mapNotNull { record ->
                serializer.tryDeserialize<SignRpc.SessionRequest>(record.body)?.toRequest(record)
                    ?.toSessionRequest(metadataStorageRepository.getByTopicAndType(Topic(record.topic), AppMetaDataType.PEER))
            }
    }
}

internal interface GetPendingSessionRequestByTopicUseCaseInterface {
    suspend fun getPendingSessionRequests(topic: Topic): List<EngineDO.SessionRequest>
}