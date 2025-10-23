package app.perawallet.walletconnectv2.internal.common.signing.message

import app.perawallet.walletconnectv2.cacao.signature.SignatureType
import app.perawallet.walletconnectv2.internal.common.model.ProjectId
import app.perawallet.walletconnectv2.internal.common.signing.signature.Signature
import app.perawallet.walletconnectv2.internal.common.signing.signature.verify


class MessageSignatureVerifier(private val projectId: ProjectId) {
    fun verify(signature: String, originalMessage: String, address: String, type: SignatureType): Boolean =
        Signature.fromString(signature).verify(originalMessage, address, type.header, projectId)
}