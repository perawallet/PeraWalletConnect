package app.perawallet.walletconnectv2.foundation.common.model

import app.perawallet.walletconnectv2.foundation.util.hexToBytes

interface Key {
    val keyAsHex: String
    val keyAsBytes: ByteArray
        get() = keyAsHex.hexToBytes()
}

@JvmInline
value class PublicKey(override val keyAsHex: String) : Key

@JvmInline
value class PrivateKey(override val keyAsHex: String) : Key