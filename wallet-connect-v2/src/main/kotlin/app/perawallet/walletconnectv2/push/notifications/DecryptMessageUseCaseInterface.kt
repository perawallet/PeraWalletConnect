package app.perawallet.walletconnectv2.push.notifications

import app.perawallet.walletconnectv2.Core

interface DecryptMessageUseCaseInterface {
    suspend fun decryptNotification(topic: String, message: String, onSuccess: (Core.Model.Message) -> Unit, onFailure: (Throwable) -> Unit)
}