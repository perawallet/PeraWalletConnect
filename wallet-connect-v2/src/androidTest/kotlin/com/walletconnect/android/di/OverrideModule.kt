package app.perawallet.walletconnectv2.di

import app.perawallet.walletconnectv2.internal.common.di.coreCryptoModule
import app.perawallet.walletconnectv2.internal.common.di.coreJsonRpcModule
import app.perawallet.walletconnectv2.internal.common.di.corePairingModule
import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.relay.RelayConnectionInterface
import org.koin.dsl.module

private const val SHARED_PREFS_FILE = "wc_key_store"
private const val KEY_STORE_ALIAS = "wc_keystore_key"

// When called more that once, different `storagePrefix` must be defined.

internal fun overrideModule(
    relay: RelayConnectionInterface,
    pairing: PairingInterface,
    pairingController: PairingControllerInterface,
    storagePrefix: String,
    bundleId: String
) = module {
    val sharedPrefsFile = storagePrefix + SHARED_PREFS_FILE
    val keyStoreAlias = storagePrefix + KEY_STORE_ALIAS

    single { relay }

    includes(
        coreStorageModule(storagePrefix, bundleId),
        corePairingModule(pairing, pairingController),
        coreCryptoModule(sharedPrefsFile, keyStoreAlias),
        coreJsonRpcModule()
    )
}