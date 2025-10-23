package app.perawallet.walletconnectv2.verify.client

import app.perawallet.walletconnectv2.verify.domain.VerifyResult

interface VerifyInterface {
    fun initialize()
    fun register(attestationId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit)
    fun resolve(attestationId: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit)
    fun resolveV2(attestationId: String, attestationJWT: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit)
}