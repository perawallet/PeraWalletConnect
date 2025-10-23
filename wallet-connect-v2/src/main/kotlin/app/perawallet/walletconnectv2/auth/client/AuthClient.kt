package app.perawallet.walletconnectv2.auth.client

object AuthClient : AuthInterface by AuthProtocol.instance {
    interface RequesterDelegate : AuthInterface.RequesterDelegate
    interface ResponderDelegate : AuthInterface.ResponderDelegate
}