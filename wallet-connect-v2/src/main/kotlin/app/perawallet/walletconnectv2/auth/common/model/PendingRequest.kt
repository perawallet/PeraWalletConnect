package app.perawallet.walletconnectv2.auth.common.model

internal data class PendingRequest(
    val id: Long,
    val pairingTopic: String,
    val payloadParams: PayloadParams
)
