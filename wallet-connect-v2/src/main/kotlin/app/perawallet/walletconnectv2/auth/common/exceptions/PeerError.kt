@file:JvmSynthetic

package app.perawallet.walletconnectv2.auth.common.exceptions

import app.perawallet.walletconnectv2.internal.common.model.type.Error

internal sealed class PeerError : Error {
    object SignatureVerificationFailed : PeerError() {
        override val message: String = "Signature verification failed"
        override val code: Int = 11004
    }
}