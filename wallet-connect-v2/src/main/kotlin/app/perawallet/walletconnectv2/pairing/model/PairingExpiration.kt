@file:JvmName("Expiration")

package app.perawallet.walletconnectv2.pairing.model

import app.perawallet.walletconnectv2.internal.utils.currentTimeInSeconds
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds

val pairingExpiry: Long get() = currentTimeInSeconds + fiveMinutesInSeconds