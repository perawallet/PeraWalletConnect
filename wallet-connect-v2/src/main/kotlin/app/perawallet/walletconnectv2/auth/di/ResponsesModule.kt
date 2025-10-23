package app.perawallet.walletconnectv2.auth.di

import app.perawallet.walletconnectv2.auth.use_case.responses.OnAuthRequestResponseUseCase
import org.koin.dsl.module


internal fun responsesModule() = module {
    single { OnAuthRequestResponseUseCase(cacaoVerifier = get(), pairingHandler = get(), pairingInterface = get()) }
}