package app.perawallet.walletconnectv2.keyserver.model

import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao

sealed class KeyServerResponse {

    @JsonClass(generateAdapter = true)
    data class ResolveInvite(val inviteKey: String) : KeyServerResponse()

    @JsonClass(generateAdapter = true)
    data class ResolveIdentity(val cacao: Cacao) : KeyServerResponse()
}