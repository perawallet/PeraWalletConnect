package app.perawallet.walletconnectv2.keyserver.domain.use_case

import app.perawallet.walletconnectv2.keyserver.data.service.KeyServerService
import app.perawallet.walletconnectv2.keyserver.model.KeyServerBody

class RegisterInviteUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(idAuth: String): Result<Unit> = runCatching {
        service.registerInvite(KeyServerBody.RegisterInvite(idAuth)).unwrapUnit()
    }
}