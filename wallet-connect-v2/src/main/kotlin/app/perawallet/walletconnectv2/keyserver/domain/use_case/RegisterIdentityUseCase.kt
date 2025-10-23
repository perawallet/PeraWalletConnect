package app.perawallet.walletconnectv2.keyserver.domain.use_case

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.keyserver.data.service.KeyServerService
import app.perawallet.walletconnectv2.keyserver.model.KeyServerBody

class RegisterIdentityUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(cacao: Cacao): Result<Unit> = runCatching {
        service.registerIdentity(KeyServerBody.RegisterIdentity(cacao)).unwrapUnit()
    }
}