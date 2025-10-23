package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.verify.model.VerifyContext
import kotlinx.coroutines.supervisorScope

internal class GetListOfVerifyContextsUseCase(private val verifyContextStorageRepository: VerifyContextStorageRepository) : GetListOfVerifyContextsUseCaseInterface {
    override suspend fun getListOfVerifyContext(): List<VerifyContext> = supervisorScope { verifyContextStorageRepository.getAll() }
}

internal interface GetListOfVerifyContextsUseCaseInterface {
    suspend fun getListOfVerifyContext(): List<VerifyContext>
}
