package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.foundation.common.model.Ttl

data class IrnParams(val tag: Tags, val ttl: Ttl, val prompt: Boolean = false)