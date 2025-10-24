

package app.perawallet.walletconnectv2.internal.common.di

import app.perawallet.walletconnectv2.internal.common.explorer.ExplorerRepository
import app.perawallet.walletconnectv2.internal.common.explorer.data.network.ExplorerService
import app.perawallet.walletconnectv2.internal.common.explorer.domain.usecase.GetNotifyConfigUseCase
import app.perawallet.walletconnectv2.internal.common.explorer.domain.usecase.GetProjectsWithPaginationUseCase
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory


internal fun explorerModule() = module {

    single(named(AndroidCommonDITags.EXPLORER_URL)) { "https://registry.walletconnect.org/" }

    single(named(AndroidCommonDITags.EXPLORER_RETROFIT)) {
        Retrofit.Builder()
            .baseUrl(get<String>(named(AndroidCommonDITags.EXPLORER_URL)))
            .client(get(named(AndroidCommonDITags.OK_HTTP)))
            .addConverterFactory(MoshiConverterFactory.create(get(named(AndroidCommonDITags.MOSHI))))
            .build()
    }

    single { get<Retrofit>(named(AndroidCommonDITags.EXPLORER_RETROFIT)).create(ExplorerService::class.java) }

    single {
        ExplorerRepository(
            explorerService = get(),
            projectId = get(),
        )
    }

    single { GetProjectsWithPaginationUseCase(get()) }
    single { GetNotifyConfigUseCase(get()) }
}