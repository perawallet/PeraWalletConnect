@file:JvmSynthetic

package app.perawallet.walletconnectv2.internal.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val job = SupervisorJob()

@JvmSynthetic
var scope = CoroutineScope(job + Dispatchers.IO)