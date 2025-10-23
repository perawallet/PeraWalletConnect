package app.perawallet.walletconnectv2.verify.model

import app.perawallet.walletconnectv2.internal.common.model.Validation

data class VerifyContext(
    val id: Long,
    val origin: String,
    val validation: Validation,
    val verifyUrl: String,
    val isScam: Boolean?
)