@file:JvmSynthetic

package app.perawallet.walletconnectv2.auth.common.model

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao

internal sealed class AuthResponse {
    data class Result(val cacao: Cacao) : AuthResponse()
    data class Error(val code: Int, val message: String) : AuthResponse()
}