package app.perawallet.walletconnectv2.keyserver.domain

import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.AccountHasDifferentStatementStored
import app.perawallet.walletconnectv2.internal.common.exception.AccountHasNoCacaoPayloadStored
import app.perawallet.walletconnectv2.internal.common.exception.AccountHasNoIdentityStored
import app.perawallet.walletconnectv2.internal.common.exception.InvalidAccountIdException
import app.perawallet.walletconnectv2.internal.common.exception.InvalidIdentityCacao
import app.perawallet.walletconnectv2.internal.common.exception.UserRejectedSigning
import app.perawallet.walletconnectv2.internal.common.jwt.did.EncodeDidJwtPayloadUseCase
import app.perawallet.walletconnectv2.internal.common.jwt.did.EncodeIdentityKeyDidJwtPayloadUseCase
import app.perawallet.walletconnectv2.internal.common.jwt.did.encodeDidJwt
import app.perawallet.walletconnectv2.internal.common.model.AccountId
import app.perawallet.walletconnectv2.internal.common.model.DidJwt
import app.perawallet.walletconnectv2.internal.common.model.MissingKeyException
import app.perawallet.walletconnectv2.internal.common.model.ProjectId
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoType
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoVerifier
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Issuer
import app.perawallet.walletconnectv2.internal.common.signing.cacao.getStatement
import app.perawallet.walletconnectv2.internal.common.signing.cacao.toCAIP222Message
import app.perawallet.walletconnectv2.internal.common.storage.identity.IdentitiesStorageRepository
import app.perawallet.walletconnectv2.internal.utils.getIdentityTag
import app.perawallet.walletconnectv2.keyserver.domain.use_case.RegisterIdentityUseCase
import app.perawallet.walletconnectv2.keyserver.domain.use_case.ResolveIdentityUseCase
import app.perawallet.walletconnectv2.keyserver.domain.use_case.UnregisterIdentityUseCase
import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.foundation.util.jwt.DID_DELIMITER
import app.perawallet.walletconnectv2.foundation.util.jwt.decodeDidPkh
import app.perawallet.walletconnectv2.foundation.util.jwt.encodeDidPkh
import app.perawallet.walletconnectv2.foundation.util.jwt.encodeEd25519DidKey
import app.perawallet.walletconnectv2.foundation.util.bytesToHex
import app.perawallet.walletconnectv2.foundation.util.randomBytes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IdentitiesInteractor(
    private val identitiesRepository: IdentitiesStorageRepository,
    private val resolveIdentityUseCase: ResolveIdentityUseCase,
    private val registerIdentityUseCase: RegisterIdentityUseCase,
    private val unregisterIdentityUseCase: UnregisterIdentityUseCase,
    private val projectId: ProjectId,
    private val keyManagementRepository: KeyManagementRepository,
    private val logger: Logger
) {
    fun getIdentityKeyPair(accountId: AccountId): Pair<PublicKey, PrivateKey> = keyManagementRepository.getKeyPair(getIdentityPublicKey(accountId))

    suspend fun registerIdentity(accountId: AccountId, statement: String, domain: String, resources: List<String>?, keyserverUrl: String, onSign: (String) -> Cacao.Signature?): Result<PublicKey> =
        getAlreadyRegisteredValidIdentity(accountId, statement, domain, resources)
            .recoverCatching { exception ->
                when (exception) {
                    is MissingKeyException -> generateAndStoreNewIdentity(accountId, statement, domain, resources, onSign).getOrThrow()

                    is AccountHasNoCacaoPayloadStored, is AccountHasDifferentStatementStored ->
                        handleIdentitiesOutdatedStatements(accountId, statement, domain, resources, keyserverUrl, onSign).getOrThrow()

                    else -> throw exception
                }
            }

    suspend fun registerIdentity(identityPublicKey: PublicKey, payload: Cacao.Payload, signature: Cacao.Signature): Result<Unit> =
        registerIdentityUseCase(Cacao(CacaoType.EIP4361.toHeader(), payload, signature)).onSuccess {
            val accountId = AccountId(Issuer(payload.iss).accountId)
            identitiesRepository.insertIdentity(identityPublicKey.keyAsHex, accountId, payload, isOwner = true)
            storeIdentityPublicKey(identityPublicKey, accountId)
        }

    suspend fun getAlreadyRegisteredValidIdentity(accountId: AccountId, statement: String? = null, domain: String, resources: List<String>?): Result<PublicKey> {
        if (!accountId.isValid()) throw InvalidAccountIdException(accountId)
        return runCatching {
            val storedPublicKey = getIdentityPublicKey(accountId)
            val cacaoPayload = identitiesRepository.getCacaoPayloadByIdentity(storedPublicKey.keyAsHex) ?: throw AccountHasNoCacaoPayloadStored(accountId)
            val generatedPayload = generatePayload(accountId, storedPublicKey, statement, domain, resources).getOrThrow()
            if (cacaoPayload.statement != generatedPayload.statement) throw AccountHasDifferentStatementStored(accountId)
            storedPublicKey
        }
    }

    private suspend fun generateAndStoreNewIdentity(accountId: AccountId, statement: String, domain: String, resources: List<String>?, onSign: (String) -> Cacao.Signature?): Result<PublicKey> {
        val identityPublicKey = generateAndStoreIdentityKeyPair()
        return registerIdentityKeyInKeyserver(accountId, identityPublicKey, statement, domain, resources, onSign)
            .map { identityPublicKey }
            .onSuccess { storeIdentityPublicKey(identityPublicKey, accountId) }
    }

    private suspend fun handleIdentitiesOutdatedStatements(
        accountId: AccountId, statement: String, domain: String, resources: List<String>?, keyserverUrl: String, onSign: (String) -> Cacao.Signature?,
    ): Result<PublicKey> {
        val storedKeyPair = getIdentityKeyPair(accountId)
        val (storedPublicKey, _) = storedKeyPair
        return unregisterIdentityKeyInKeyserver(accountId, keyserverUrl, storedKeyPair)
            .map { storedPublicKey }
            .onSuccess { registerIdentityKeyInKeyserver(accountId, storedPublicKey, statement, domain, resources, onSign) }
    }

    suspend fun unregisterIdentity(accountId: AccountId, keyserverUrl: String): Result<PublicKey> = try {
        if (!accountId.isValid()) throw InvalidAccountIdException(accountId)
        val storedKeyPair = getIdentityKeyPair(accountId)
        val (storedPublicKey, _) = storedKeyPair
        unregisterIdentityKeyInKeyserver(accountId, keyserverUrl, storedKeyPair)
            .map { storedPublicKey }
            .onSuccess { removeIdentityKeyPair(storedPublicKey, accountId) }
    } catch (e: MissingKeyException) {
        throw AccountHasNoIdentityStored(accountId)
    }

    private suspend fun resolveIdentity(identityKey: String): Result<AccountId> = resolveIdentityLocally(identityKey).recover { resolveAndStoreIdentityRemotely(identityKey).getOrThrow() }

    suspend fun resolveIdentityDidKey(identityDidKey: String): Result<AccountId> = resolveIdentity(identityDidKey.split(DID_DELIMITER).last())

    private suspend fun resolveIdentityLocally(identityKey: String): Result<AccountId> = runCatching { identitiesRepository.getAccountId(identityKey) }

    private suspend fun resolveAndStoreIdentityRemotely(identityKey: String) = resolveIdentityUseCase(identityKey).mapCatching { response ->
        if (!CacaoVerifier(projectId).verify(response.cacao)) throw InvalidIdentityCacao()
        val accountId = AccountId(decodeDidPkh(response.cacao.payload.iss))
        identitiesRepository.insertIdentity(identityKey, accountId, response.cacao.payload, isOwner = false)
        accountId
    }

    private fun getIdentityPublicKey(accountId: AccountId): PublicKey = keyManagementRepository.getPublicKey(accountId.getIdentityTag())

    private fun storeIdentityPublicKey(publicKey: PublicKey, accountId: AccountId) {
        keyManagementRepository.setKey(publicKey, accountId.getIdentityTag())
    }

    private fun removeIdentityKeyPair(publicKey: PublicKey, accountId: AccountId) {
        runCatching {
            keyManagementRepository.removeKeys(accountId.getIdentityTag())
        }.onFailure { logger.error(it) }

        runCatching {
            keyManagementRepository.removeKeys(publicKey.keyAsHex)
        }.onFailure { logger.error(it) }
    }

    fun generateAndStoreIdentityKeyPair(): PublicKey = keyManagementRepository.generateAndStoreEd25519KeyPair()

    private suspend fun registerIdentityKeyInKeyserver(
        accountId: AccountId,
        identityKey: PublicKey,
        statement: String,
        domain: String,
        resources: List<String>?,
        onSign: (String) -> Cacao.Signature?,
    ): Result<Unit> {
        val cacao = generateAndSignCacao(accountId, identityKey, statement, domain, resources, onSign).getOrThrow()
        return registerIdentityUseCase(cacao).onSuccess {
            identitiesRepository.insertIdentity(identityKey.keyAsHex, accountId, cacao.payload, isOwner = true)
        }
    }

    private suspend fun unregisterIdentityKeyInKeyserver(accountId: AccountId, keyserverUrl: String, identityKeyPair: Pair<PublicKey, PrivateKey>): Result<Unit> {
        return unregisterIdentityUseCase(generateUnregisterIdAuth(accountId, keyserverUrl, identityKeyPair).getOrThrow().value).onSuccess {
            identitiesRepository.removeIdentity(identityKeyPair.first.keyAsHex)
        }
    }

    private fun generateAndSignCacao(accountId: AccountId, identityKey: PublicKey, statement: String, domain: String, resources: List<String>?, onSign: (String) -> Cacao.Signature?): Result<Cacao> {
        val payload = generatePayload(accountId, identityKey, statement, domain, resources).getOrThrow()
        val message = payload.toCAIP222Message()
        val signature = onSign(message) ?: throw UserRejectedSigning()
        return Result.success(Cacao(CacaoType.EIP4361.toHeader(), payload, signature))
    }

    private fun generateUnregisterIdAuth(accountId: AccountId, keyserverUrl: String, identityKeyPair: Pair<PublicKey, PrivateKey>): Result<DidJwt> {
        val (identityPublicKey, identityPrivateKey) = identityKeyPair
        return encodeDidJwt(identityPrivateKey, EncodeIdentityKeyDidJwtPayloadUseCase(accountId), EncodeDidJwtPayloadUseCase.Params(identityPublicKey, keyserverUrl))
    }

    fun generatePayload(accountId: AccountId, identityKey: PublicKey, statement: String?, domain: String, resources: List<String>?): Result<Cacao.Payload> = Result.success(
        Cacao.Payload(
            iss = encodeDidPkh(accountId.value),
            domain = domain,
            aud = buildUri(domain, encodeEd25519DidKey(identityKey.keyAsBytes)),
            version = Cacao.Payload.CURRENT_VERSION,
            nonce = randomBytes(NONCE_SIZE).bytesToHex(),
            iat = SimpleDateFormat(Cacao.Payload.ISO_8601_PATTERN, Locale.getDefault()).format(Calendar.getInstance().time),
            nbf = null,
            exp = null,
            statement = Pair(statement, resources).getStatement(),
            requestId = null,
            resources = resources
        )
    )

    private fun buildUri(domain: String, didKey: String): String = "bundleid://$domain?walletconnect_identity_key=$didKey"

    companion object {
        const val NONCE_SIZE = 32
    }
}