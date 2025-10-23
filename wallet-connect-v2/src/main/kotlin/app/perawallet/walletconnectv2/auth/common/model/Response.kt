package app.perawallet.walletconnectv2.auth.common.model

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao

internal sealed class Response {
    abstract val id: Long

    data class Result(override val id: Long, val cacao: Cacao) : Response()
    data class Error(override val id: Long, val code: Int, val message: String) : Response()
}
