@file:JvmName("Expiration")

package app.perawallet.walletconnectv2.internal.utils

val PROPOSAL_EXPIRY: Long get() = currentTimeInSeconds + fiveMinutesInSeconds
val ACTIVE_SESSION: Long get() = currentTimeInSeconds + weekInSeconds