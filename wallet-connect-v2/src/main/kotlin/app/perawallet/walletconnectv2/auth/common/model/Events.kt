@file:JvmSynthetic

package app.perawallet.walletconnectv2.auth.common.model

import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.verify.model.VerifyContext

internal sealed class Events : EngineEvent {
    data class OnAuthRequest(val id: Long, val pairingTopic: String, val payloadParams: PayloadParams, val verifyContext: VerifyContext) : Events()
    data class OnAuthResponse(val id: Long, val response: AuthResponse) : Events()
}