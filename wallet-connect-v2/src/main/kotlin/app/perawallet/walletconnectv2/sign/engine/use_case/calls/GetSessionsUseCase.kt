package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import app.perawallet.walletconnectv2.internal.utils.isSequenceValid
import kotlinx.coroutines.supervisorScope

internal class GetSessionsUseCase(
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val sessionStorageRepository: SessionStorageRepository,
    private val selfAppMetaData: AppMetaData
) : GetSessionsUseCaseInterface {

    override suspend fun getListOfSettledSessions(): List<EngineDO.Session> = supervisorScope {
        return@supervisorScope sessionStorageRepository.getListOfSessionVOsWithoutMetadata()
            .filter { session -> session.isAcknowledged && session.expiry.isSequenceValid() }
            .map { session -> session.copy(selfAppMetaData = selfAppMetaData, peerAppMetaData = metadataStorageRepository.getByTopicAndType(session.topic, AppMetaDataType.PEER)) }
            .map { session -> session.toEngineDO() }
    }
}

internal interface GetSessionsUseCaseInterface {
    suspend fun getListOfSettledSessions(): List<EngineDO.Session>
}