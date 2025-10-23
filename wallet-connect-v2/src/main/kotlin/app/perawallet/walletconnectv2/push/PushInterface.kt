package app.perawallet.walletconnectv2.push

interface PushInterface {
    val clientId: String

    fun register(firebaseAccessToken: String, enableEncrypted: Boolean = false, onSuccess: () -> Unit, onError: (Throwable) -> Unit)

    fun unregister(onSuccess: () -> Unit, onError: (Throwable) -> Unit)
}