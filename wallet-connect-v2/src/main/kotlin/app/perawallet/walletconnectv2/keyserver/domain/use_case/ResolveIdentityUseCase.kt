package app.perawallet.walletconnectv2.keyserver.domain.use_case

import app.perawallet.walletconnectv2.keyserver.data.service.KeyServerService
import app.perawallet.walletconnectv2.keyserver.model.KeyServerResponse

class ResolveIdentityUseCase(
    private val service: KeyServerService,
) {
    suspend operator fun invoke(identityKey: String): Result<KeyServerResponse.ResolveIdentity> = runCatching {
        service.resolveIdentity(identityKey).unwrapValue()
    }
}