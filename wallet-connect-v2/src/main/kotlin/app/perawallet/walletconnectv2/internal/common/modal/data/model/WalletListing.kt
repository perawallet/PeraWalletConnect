package app.perawallet.walletconnectv2.internal.common.modal.data.model

data class WalletListing(
    val page: Int,
    val totalCount: Int,
    val wallets: List<Wallet>
)
