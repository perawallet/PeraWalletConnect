package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.foundation.common.model.PublicKey

data class Participants(
    val senderPublicKey: PublicKey,
    val receiverPublicKey: PublicKey,
)