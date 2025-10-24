

package app.perawallet.walletconnectv2.foundation.di

import app.perawallet.walletconnectv2.foundation.crypto.data.repository.ClientIdJwtRepository
import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.crypto.data.repository.BaseClientIdJwtRepository
import org.koin.dsl.module

internal fun cryptoModule() = module {

    single<ClientIdJwtRepository> {
        object: BaseClientIdJwtRepository() {
            override fun setKeyPair(key: String, privateKey: PrivateKey, publicKey: PublicKey) {}
        }
    }
}
