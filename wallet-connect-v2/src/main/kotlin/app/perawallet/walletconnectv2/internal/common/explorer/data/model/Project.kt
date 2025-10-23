package app.perawallet.walletconnectv2.internal.common.explorer.data.model

data class Project(
    val id: String,
    val name: String,
    val homepage: String,
    val imageId: String,
    val description: String,
    val imageUrl: ImageUrl,
    val dappUrl: String,
    val order: Long?
)