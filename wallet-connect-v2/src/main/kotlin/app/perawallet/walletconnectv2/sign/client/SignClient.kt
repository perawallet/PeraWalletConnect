package app.perawallet.walletconnectv2.sign.client

object SignClient : SignInterface by SignProtocol.instance {
    interface WalletDelegate: SignInterface.WalletDelegate
    interface DappDelegate: SignInterface.DappDelegate
}