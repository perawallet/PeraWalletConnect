package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.foundation.common.model.Key

@JvmInline
value class SymmetricKey(override val keyAsHex: String) : Key