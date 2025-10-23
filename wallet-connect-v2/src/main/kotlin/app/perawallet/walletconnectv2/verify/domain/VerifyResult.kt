package app.perawallet.walletconnectv2.verify.domain

import app.perawallet.walletconnectv2.internal.common.model.Validation

data class VerifyResult(
    val validation: Validation,
    val isScam: Boolean?,
    val origin: String
)