@file:JvmSynthetic

package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.utils.CoreValidator


@JvmInline
value class AccountId(val value: String) {
    fun isValid(): Boolean = CoreValidator.isAccountIdCAIP10Compliant(value)
    fun address() = value.split(":").last()
}