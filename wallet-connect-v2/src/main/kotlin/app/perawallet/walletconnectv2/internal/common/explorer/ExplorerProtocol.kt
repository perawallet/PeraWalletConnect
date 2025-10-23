package app.perawallet.walletconnectv2.internal.common.explorer

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.Project
import app.perawallet.walletconnectv2.internal.common.explorer.domain.usecase.GetProjectsWithPaginationUseCase
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.foundation.util.Logger
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named


//discuss: Opening more endpoints to SDK consumers
class ExplorerProtocol(
    private val koinApp: KoinApplication = wcKoinApp,
) : ExplorerInterface {
    private val getProjectsWithPaginationUseCase: GetProjectsWithPaginationUseCase by lazy { koinApp.koin.get() }
    private val logger: Logger by lazy { koinApp.koin.get(named(AndroidCommonDITags.LOGGER)) }

    override suspend fun getProjects(page: Int, entries: Int, isVerified: Boolean, isFeatured: Boolean): Result<List<Project>> = getProjectsWithPaginationUseCase(page, entries, isVerified, isFeatured)
}


