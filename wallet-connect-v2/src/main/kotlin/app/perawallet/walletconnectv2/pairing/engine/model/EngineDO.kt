

package app.perawallet.walletconnectv2.pairing.engine.model

import app.perawallet.walletconnectv2.internal.common.model.Pairing

internal sealed class EngineDO {

    data class PairingDelete(
        val topic: String,
        val reason: String,
    ) : EngineDO()

    data class PairingExpire(
        val pairing: Pairing
    ) : EngineDO()

    data class PairingState(
        val isPairingState: Boolean
    ) : EngineDO()
}