package app.perawallet.walletconnectv2.internal.common.storage.metadata

import app.cash.sqldelight.db.QueryResult
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.foundation.common.model.Topic

interface MetadataStorageRepositoryInterface {

    fun insertOrAbortMetadata(topic: Topic, appMetaData: AppMetaData, appMetaDataType: AppMetaDataType): QueryResult<Long>

    fun updateMetaData(topic: Topic, appMetaData: AppMetaData, appMetaDataType: AppMetaDataType): QueryResult<Long>

    suspend fun updateOrAbortMetaDataTopic(oldTopic: Topic, newTopic: Topic)

    fun deleteMetaData(topic: Topic): QueryResult<Long>

    fun existsByTopicAndType(topic: Topic, type: AppMetaDataType): Boolean

    fun getByTopicAndType(topic: Topic, type: AppMetaDataType): AppMetaData?

    fun upsertPeerMetadata(topic: Topic, appMetaData: AppMetaData, appMetaDataType: AppMetaDataType)
}