package app.perawallet.walletconnectv2.keyserver.domain.use_case

import app.perawallet.walletconnectv2.internal.common.model.AccountId
import app.perawallet.walletconnectv2.keyserver.data.service.KeyServerService
import app.perawallet.walletconnectv2.keyserver.model.KeyServerResponse

class ResolveInviteUseCase(
    private val service: KeyServerService
) {
    suspend operator fun invoke(accountId: AccountId): Result<KeyServerResponse.ResolveInvite> = runCatching {
        service.resolveInvite(accountId.value).unwrapValue()
    }
}