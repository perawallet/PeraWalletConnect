package app.perawallet.walletconnectv2.auth.di

import app.perawallet.walletconnectv2.auth.use_case.requests.OnAuthRequestUseCase
import org.koin.dsl.module


internal fun requestsModule() = module {
    single { OnAuthRequestUseCase(jsonRpcInteractor = get(), resolveAttestationIdUseCase = get(), pairingController = get()) }
}
