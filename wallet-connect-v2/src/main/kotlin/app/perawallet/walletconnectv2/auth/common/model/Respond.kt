

package app.perawallet.walletconnectv2.auth.common.model

import app.perawallet.walletconnectv2.auth.client.Auth

internal sealed class Respond {
    abstract val id: Long

    data class Result(override val id: Long, val signature: Auth.Model.Cacao.Signature, val iss: String) : Respond()
    data class Error(override val id: Long, val code: Int, val message: String) : Respond()
}