package app.perawallet.walletconnectv2

import android.app.Application
import app.perawallet.walletconnectv2.internal.common.explorer.ExplorerInterface
import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.push.PushInterface
import app.perawallet.walletconnectv2.relay.ConnectionType
import app.perawallet.walletconnectv2.relay.NetworkClientTimeout
import app.perawallet.walletconnectv2.relay.RelayConnectionInterface
import app.perawallet.walletconnectv2.verify.client.VerifyInterface

interface CoreInterface {
    val Pairing: PairingInterface
    val PairingController: PairingControllerInterface
    val Relay: RelayConnectionInterface
    val Echo: PushInterface
    val Push: PushInterface
    val Verify: VerifyInterface
    val Explorer: ExplorerInterface

    interface Delegate : PairingInterface.Delegate

    fun setDelegate(delegate: Delegate)

    fun initialize(
        metaData: Core.Model.AppMetaData,
        relayServerUrl: String,
        connectionType: ConnectionType = ConnectionType.AUTOMATIC,
        application: Application,
        relay: RelayConnectionInterface? = null,
        keyServerUrl: String? = null,
        networkClientTimeout: NetworkClientTimeout? = null,
        telemetryEnabled: Boolean = true,
        onError: (Core.Model.Error) -> Unit,
    )

    fun initialize(
        application: Application,
        projectId: String,
        metaData: Core.Model.AppMetaData,
        connectionType: ConnectionType = ConnectionType.AUTOMATIC,
        relay: RelayConnectionInterface? = null,
        keyServerUrl: String? = null,
        networkClientTimeout: NetworkClientTimeout? = null,
        telemetryEnabled: Boolean = true,
        onError: (Core.Model.Error) -> Unit,
    )
}