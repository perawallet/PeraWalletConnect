package app.perawallet.walletconnectv2.internal.common.storage.pairing

import app.cash.sqldelight.db.QueryResult
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.Pairing
import app.perawallet.walletconnectv2.foundation.common.model.Topic

interface PairingStorageRepositoryInterface {

    fun insertPairing(pairing: Pairing)

    fun deletePairing(topic: Topic)

    fun hasTopic(topic: Topic): Boolean

    suspend fun getListOfPairings(): List<Pairing>

    suspend fun getListOfPairingsWithoutRequestReceived(): List<Pairing>

    fun setRequestReceived(topic: Topic)

    fun updateExpiry(topic: Topic, expiry: Expiry) : QueryResult<Long>

    fun getPairingOrNullByTopic(topic: Topic): Pairing?
}