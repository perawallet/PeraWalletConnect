package app.perawallet.walletconnectv2.internal.common.storage.identity

import com.squareup.moshi.Moshi
import app.perawallet.walletconnectv2.internal.common.model.AccountId
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.sdk.storage.`data`.dao.IdentitiesQueries

class IdentitiesStorageRepository(private val identities: IdentitiesQueries, moshiBuilder: Moshi.Builder) {
    private val moshi = moshiBuilder.build()

    suspend fun insertIdentity(identityPublicKey: String, accountId: AccountId, cacaoPayload: Cacao.Payload, isOwner: Boolean) =
        identities.insertOrAbortIdentity(identityPublicKey, accountId.value, moshi.adapter(Cacao.Payload::class.java).toJson(cacaoPayload), isOwner)

    suspend fun removeIdentity(identityPublicKey: String) = identities.removeIdentity(identityPublicKey)

    suspend fun getAccountId(identityPublicKey: String) = AccountId(identities.getAccountIdByIdentity(identityPublicKey).executeAsOne())

    suspend fun getCacaoPayloadByIdentity(identityPublicKey: String): Cacao.Payload? =
        runCatching { identities.getCacaoPayloadByIdentity(identityPublicKey).executeAsOne().cacao_payload?.let { moshi.adapter(Cacao.Payload::class.java).fromJson(it) } }.getOrNull()
}
