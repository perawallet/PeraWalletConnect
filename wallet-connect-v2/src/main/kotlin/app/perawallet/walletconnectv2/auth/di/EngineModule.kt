package app.perawallet.walletconnectv2.auth.di

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoVerifier
import app.perawallet.walletconnectv2.auth.engine.domain.AuthEngine
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntriesUseCase
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntriesUseCaseInterface
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntryByIdUseCase
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntryByTopicUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmSynthetic
internal fun engineModule() = module {

    includes(callsModule(), requestsModule(), responsesModule())

    single<GetPendingJsonRpcHistoryEntriesUseCaseInterface> { GetPendingJsonRpcHistoryEntriesUseCase(jsonRpcHistory = get(), serializer = get()) }
    single { GetPendingJsonRpcHistoryEntryByIdUseCase(jsonRpcHistory = get(), serializer = get()) }
    single { GetPendingJsonRpcHistoryEntryByTopicUseCase(jsonRpcHistory = get(), serializer = get()) }
    single { CacaoVerifier(projectId = get()) }
    single {
        AuthEngine(
            jsonRpcInteractor = get(),
            verifyContextStorageRepository = get(),
            getListOfVerifyContextsUseCase = get(),
            getVerifyContextUseCase = get(),
            formatMessageUseCase = get(),
            onAuthRequestUseCase = get(),
            onAuthRequestResponseUseCase = get(),
            respondAuthRequestUseCase = get(),
            sendAuthRequestUseCase = get(),
            pairingHandler = get(),
            getPendingJsonRpcHistoryEntriesUseCase = get(),
            getPendingJsonRpcHistoryEntryByTopicUseCase = get(),
            decryptAuthMessageUseCase = get(named(AndroidCommonDITags.DECRYPT_AUTH_MESSAGE)),
        )
    }
}