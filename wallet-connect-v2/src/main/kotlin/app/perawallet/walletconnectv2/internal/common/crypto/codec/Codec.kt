package app.perawallet.walletconnectv2.internal.common.crypto.codec

import app.perawallet.walletconnectv2.internal.common.model.EnvelopeType
import app.perawallet.walletconnectv2.internal.common.model.Participants
import app.perawallet.walletconnectv2.foundation.common.model.Topic

interface Codec {
    fun encrypt(topic: Topic, payload: String, envelopeType: EnvelopeType, participants: Participants? = null): ByteArray
    fun decrypt(topic: Topic, cipherText: ByteArray): String
}