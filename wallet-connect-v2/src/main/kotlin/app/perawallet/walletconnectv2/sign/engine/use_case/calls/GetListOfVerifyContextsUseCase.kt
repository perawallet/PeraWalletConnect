package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO

internal class GetListOfVerifyContextsUseCase(private val verifyContextStorageRepository: VerifyContextStorageRepository) : GetListOfVerifyContextsUseCaseInterface {
    override suspend fun getListOfVerifyContexts(): List<EngineDO.VerifyContext> = verifyContextStorageRepository.getAll().map { verifyContext -> verifyContext.toEngineDO() }
}

internal interface GetListOfVerifyContextsUseCaseInterface {
    suspend fun getListOfVerifyContexts(): List<EngineDO.VerifyContext>
}