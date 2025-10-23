package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.verify.model.VerifyContext
import kotlinx.coroutines.supervisorScope

internal class GetVerifyContextUseCase(private val verifyContextStorageRepository: VerifyContextStorageRepository) : GetVerifyContextUseCaseInterface {
    override suspend fun getVerifyContext(id: Long): VerifyContext? = supervisorScope { verifyContextStorageRepository.get(id) }
}

internal interface GetVerifyContextUseCaseInterface {
    suspend fun getVerifyContext(id: Long): VerifyContext?
}