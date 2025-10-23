package app.perawallet.walletconnectv2.internal.common.explorer.data.model

data class SupportedStandard(
    val id: String,
    val url: String,
    val title: String,
    val standardId: Int,
    val standardPrefix: String
)