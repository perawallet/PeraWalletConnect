package app.perawallet.walletconnectv2.internal.common.explorer.data.model

data class DappListings(
    val listings: List<Listing>,
    val count: Int,
    val total: Int
)