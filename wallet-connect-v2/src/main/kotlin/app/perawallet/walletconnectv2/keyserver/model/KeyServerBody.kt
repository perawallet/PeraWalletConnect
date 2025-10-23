package app.perawallet.walletconnectv2.keyserver.model

import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao

sealed class KeyServerBody {

    @JsonClass(generateAdapter = true)
    data class UnregisterIdentity(val idAuth: String) : KeyServerBody()

    @JsonClass(generateAdapter = true)
    data class RegisterIdentity(val cacao: Cacao) : KeyServerBody()

    @JsonClass(generateAdapter = true)
    data class RegisterInvite(val idAuth: String) : KeyServerBody()
    @JsonClass(generateAdapter = true)
    data class UnregisterInvite(val idAuth: String) : KeyServerBody()
}