package app.perawallet.walletconnectv2.sign.storage.authenticate

import android.database.sqlite.SQLiteException
import app.perawallet.walletconnectv2.sign.storage.`data`.dao.authenticatereponse.AuthenticateResponseTopicDaoQueries

internal class AuthenticateResponseTopicRepository(private val authenticateResponseTopicDaoQueries: AuthenticateResponseTopicDaoQueries) {
    
    @Throws(SQLiteException::class)
    suspend fun insertOrAbort(pairingTopic: String, responseTopic: String) {
        authenticateResponseTopicDaoQueries.insertOrAbort(pairingTopic, responseTopic)
    }

    
    @Throws(SQLiteException::class)
    suspend fun delete(pairingTopic: String) {
        authenticateResponseTopicDaoQueries.deleteByPairingTopic(pairingTopic)
    }

    
    @Throws(SQLiteException::class)
    suspend fun getResponseTopics(): List<String> {
        return authenticateResponseTopicDaoQueries.getListOfTopics().executeAsList().map { it.responseTopic }
    }
}