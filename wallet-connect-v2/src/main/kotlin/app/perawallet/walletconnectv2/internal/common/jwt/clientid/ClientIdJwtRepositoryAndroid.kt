

package app.perawallet.walletconnectv2.internal.common.jwt.clientid

import app.perawallet.walletconnectv2.internal.common.exception.CannotFindKeyPairException
import app.perawallet.walletconnectv2.internal.common.storage.key_chain.KeyStore
import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.crypto.data.repository.BaseClientIdJwtRepository

internal class ClientIdJwtRepositoryAndroid(private val keyChain: KeyStore) : BaseClientIdJwtRepository() {

    override fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {
        keyChain.setKeys(CLIENT_ID_KEYPAIR_TAG, privateKey, publicKey)
    }

    override fun getKeyPair(): Pair<String, String> {
        return if (doesKeyPairExist()) {
            val (privateKey, publicKey) = keyChain.getKeys(CLIENT_ID_KEYPAIR_TAG)
                ?: throw CannotFindKeyPairException("No key pair for given tag: $CLIENT_ID_KEYPAIR_TAG")
            publicKey to privateKey
        } else {
            generateAndStoreClientIdKeyPair()
        }
    }

    private fun doesKeyPairExist(): Boolean {
        return keyChain.checkKeys(CLIENT_ID_KEYPAIR_TAG)
    }
}