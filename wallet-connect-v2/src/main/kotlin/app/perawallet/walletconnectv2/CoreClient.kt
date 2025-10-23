package app.perawallet.walletconnectv2

object CoreClient : CoreInterface by CoreProtocol.instance {

    interface CoreDelegate : CoreInterface.Delegate
}