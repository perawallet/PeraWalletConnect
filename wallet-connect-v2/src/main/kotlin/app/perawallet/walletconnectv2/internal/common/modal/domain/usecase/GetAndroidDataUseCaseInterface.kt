package app.perawallet.walletconnectv2.internal.common.modal.domain.usecase

import app.perawallet.walletconnectv2.internal.common.modal.Web3ModalApiRepository

interface GetInstalledWalletsIdsUseCaseInterface {
    suspend operator fun invoke(
        sdkType: String
    ): List<String>
}

internal class GetInstalledWalletsIdsUseCase(
    private val web3ModalApiRepository: Web3ModalApiRepository
) : GetInstalledWalletsIdsUseCaseInterface {
    override suspend fun invoke(sdkType: String): List<String> = web3ModalApiRepository.getAndroidWalletsData(sdkType).map { it.map { walletAppData -> walletAppData.id } }.getOrThrow()
}
