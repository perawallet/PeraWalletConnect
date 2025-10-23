package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO

internal class GetVerifyContextByIdUseCase(private val verifyContextStorageRepository: VerifyContextStorageRepository) : GetVerifyContextByIdUseCaseInterface {
    override suspend fun getVerifyContext(id: Long): EngineDO.VerifyContext? = verifyContextStorageRepository.get(id)?.toEngineDO()
}

internal interface GetVerifyContextByIdUseCaseInterface {
    suspend fun getVerifyContext(id: Long): EngineDO.VerifyContext?
}