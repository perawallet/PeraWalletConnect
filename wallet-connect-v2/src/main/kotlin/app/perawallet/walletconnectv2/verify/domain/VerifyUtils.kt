package app.perawallet.walletconnectv2.verify.domain

import app.perawallet.walletconnectv2.internal.common.model.Validation
import app.perawallet.walletconnectv2.internal.utils.compareDomains

fun getValidation(metadataUrl: String, origin: String) = if (compareDomains(metadataUrl, origin)) Validation.VALID else Validation.INVALID