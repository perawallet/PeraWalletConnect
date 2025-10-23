package app.perawallet.walletconnectv2.internal.common.model.type

import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.foundation.common.model.Topic

interface Sequence {
    val topic: Topic
    val expiry: Expiry
}