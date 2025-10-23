package app.perawallet.walletconnectv2.internal.common.modal.domain.usecase

import app.perawallet.walletconnectv2.internal.common.modal.Web3ModalApiRepository
import kotlinx.coroutines.runBlocking

interface EnableAnalyticsUseCaseInterface {
    fun fetchAnalyticsConfig(): Boolean
}

internal class EnableAnalyticsUseCase(private val repository: Web3ModalApiRepository) : EnableAnalyticsUseCaseInterface {
    override fun fetchAnalyticsConfig(): Boolean {
        return runBlocking {
            try {
                val response = repository.getAnalyticsConfig()
                if (response.isSuccess) {
                    response.getOrDefault(false)
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}