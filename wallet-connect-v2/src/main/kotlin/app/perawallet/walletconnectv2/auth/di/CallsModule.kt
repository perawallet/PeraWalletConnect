package app.perawallet.walletconnectv2.auth.di

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.push.notifications.DecryptMessageUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.DecryptAuthMessageUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.FormatMessageUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.FormatMessageUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.GetListOfVerifyContextsUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.GetListOfVerifyContextsUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.GetVerifyContextUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.GetVerifyContextUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.RespondAuthRequestUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.RespondAuthRequestUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.SendAuthRequestUseCase
import app.perawallet.walletconnectv2.auth.use_case.calls.SendAuthRequestUseCaseInterface
import org.koin.core.qualifier.named
import org.koin.dsl.module


internal fun callsModule() = module {

    single<SendAuthRequestUseCaseInterface> { SendAuthRequestUseCase(crypto = get(), jsonRpcInteractor = get(), selfAppMetaData = get(), logger = get(named(AndroidCommonDITags.LOGGER))) }

    single<RespondAuthRequestUseCaseInterface> {
        RespondAuthRequestUseCase(
            crypto = get(),
            jsonRpcInteractor = get(),
            verifyContextStorageRepository = get(),
            getPendingJsonRpcHistoryEntryByIdUseCase = get(),
            cacaoVerifier = get(),
            pairingController = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single<DecryptMessageUseCaseInterface>(named(AndroidCommonDITags.DECRYPT_AUTH_MESSAGE)) {
        val useCase = DecryptAuthMessageUseCase(
            codec = get(),
            serializer = get(),
            metadataRepository = get(),
            pushMessageStorageRepository = get()
        )

        get<MutableMap<String, DecryptMessageUseCaseInterface>>(named(AndroidCommonDITags.DECRYPT_USE_CASES))[Tags.AUTH_REQUEST.id.toString()] = useCase
        useCase
    }

    single<FormatMessageUseCaseInterface> { FormatMessageUseCase() }

    single<GetVerifyContextUseCaseInterface> { GetVerifyContextUseCase(verifyContextStorageRepository = get()) }

    single<GetListOfVerifyContextsUseCaseInterface> { GetListOfVerifyContextsUseCase(verifyContextStorageRepository = get()) }
}