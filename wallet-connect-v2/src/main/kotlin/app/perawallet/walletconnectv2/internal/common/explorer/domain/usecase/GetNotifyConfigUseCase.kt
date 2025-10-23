package app.perawallet.walletconnectv2.internal.common.explorer.domain.usecase

import app.perawallet.walletconnectv2.internal.common.explorer.ExplorerRepository
import app.perawallet.walletconnectv2.internal.common.explorer.data.model.NotifyConfig

class GetNotifyConfigUseCase(private val explorerRepository: ExplorerRepository) {
    suspend operator fun invoke(appDomain: String): Result<NotifyConfig> = runCatching { explorerRepository.getNotifyConfig(appDomain) }
}
