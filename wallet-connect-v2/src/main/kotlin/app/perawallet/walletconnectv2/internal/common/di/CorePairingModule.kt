package app.perawallet.walletconnectv2.internal.common.di

import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.engine.domain.PairingEngine
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun corePairingModule(pairing: PairingInterface, pairingController: PairingControllerInterface) = module {
    single {
        PairingEngine(
            selfMetaData = get(),
            crypto = get(),
            metadataRepository = get(),
            pairingRepository = get(),
            jsonRpcInteractor = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            insertEventUseCase = get(),
            sendBatchEventUseCase = get(),
        )
    }
    single { pairing }
    single { pairingController }
}