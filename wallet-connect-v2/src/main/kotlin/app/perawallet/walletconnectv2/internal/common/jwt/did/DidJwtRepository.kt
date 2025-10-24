
@file:JvmName("DidJwtRepository")

package app.perawallet.walletconnectv2.internal.common.jwt.did

import app.perawallet.walletconnectv2.internal.common.model.DidJwt
import app.perawallet.walletconnectv2.foundation.common.model.PrivateKey
import app.perawallet.walletconnectv2.foundation.util.jwt.*

fun <R : JwtClaims> encodeDidJwt(
    identityPrivateKey: PrivateKey,
    encodeDidJwtPayloadUseCase: EncodeDidJwtPayloadUseCase<R>,
    useCaseParams: EncodeDidJwtPayloadUseCase.Params,
): Result<DidJwt> = runCatching {
    val claims = encodeDidJwtPayloadUseCase(params = useCaseParams)
    val data = encodeData(JwtHeader.EdDSA.encoded, claims).toByteArray()
    val signature = signJwt(identityPrivateKey, data).getOrThrow()
    DidJwt(encodeJWT(JwtHeader.EdDSA.encoded, claims, signature))
}

inline fun <reified C : JwtClaims> extractVerifiedDidJwtClaims(didJwt: String): Result<C> = runCatching {
    val (header, claims, signature) = decodeJwt<C>(didJwt).getOrThrow()

    with(header) { this@with.verifyHeader() }
    with(claims) { verifyJwt(didJwt, signature) }

    claims
}

context(JwtHeader)
fun JwtHeader.verifyHeader() {
    if (algorithm != JwtHeader.EdDSA.algorithm) throw Throwable("Unsupported header alg: $algorithm")
}

context(JwtClaims)
fun JwtClaims.verifyJwt(didJwt: String, signature: String) {
    val isValid = verifySignature(decodeEd25519DidKey(issuer), extractData(didJwt).toByteArray(), signature).getOrThrow()

    if (!isValid) throw Throwable("Invalid signature")
}

