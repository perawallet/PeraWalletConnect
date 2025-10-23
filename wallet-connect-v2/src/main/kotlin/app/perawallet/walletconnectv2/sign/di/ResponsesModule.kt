package app.perawallet.walletconnectv2.sign.di

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.sign.engine.use_case.responses.OnSessionAuthenticateResponseUseCase
import app.perawallet.walletconnectv2.sign.engine.use_case.responses.OnSessionProposalResponseUseCase
import app.perawallet.walletconnectv2.sign.engine.use_case.responses.OnSessionRequestResponseUseCase
import app.perawallet.walletconnectv2.sign.engine.use_case.responses.OnSessionSettleResponseUseCase
import app.perawallet.walletconnectv2.sign.engine.use_case.responses.OnSessionUpdateResponseUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module

@JvmSynthetic
internal fun responsesModule() = module {

    single {
        OnSessionProposalResponseUseCase(
            jsonRpcInteractor = get(),
            crypto = get(),
            pairingController = get(),
            proposalStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single {
        OnSessionSettleResponseUseCase(
            crypto = get(),
            jsonRpcInteractor = get(),
            sessionStorageRepository = get(),
            metadataStorageRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER))
        )
    }

    single {
        OnSessionAuthenticateResponseUseCase(
            pairingController = get(),
            pairingInterface = get(),
            cacaoVerifier = get(),
            sessionStorageRepository = get(),
            crypto = get(),
            jsonRpcInteractor = get(),
            authenticateResponseTopicRepository = get(),
            logger = get(named(AndroidCommonDITags.LOGGER)),
            getSessionAuthenticateRequest = get(),
            metadataStorageRepository = get(),
            linkModeStorageRepository = get(),
            insertEventUseCase = get<InsertEventUseCase>(),
            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
        )
    }

    single { OnSessionUpdateResponseUseCase(sessionStorageRepository = get(), logger = get(named(AndroidCommonDITags.LOGGER))) }

    single {
        OnSessionRequestResponseUseCase(
            logger = get(named(AndroidCommonDITags.LOGGER)),
            insertEventUseCase = get<InsertEventUseCase>(),
            clientId = get(named(AndroidCommonDITags.CLIENT_ID)),
            getSessionRequestByIdUseCase = get()
        )
    }
}