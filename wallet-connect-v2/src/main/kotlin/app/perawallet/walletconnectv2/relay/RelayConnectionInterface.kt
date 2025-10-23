package app.perawallet.walletconnectv2.relay

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.foundation.network.RelayInterface
import kotlinx.coroutines.flow.StateFlow

interface RelayConnectionInterface : RelayInterface {
    val wssConnectionState: StateFlow<WSSConnectionState>
    val isNetworkAvailable: StateFlow<Boolean?>

    fun connect(onErrorModel: (Core.Model.Error) -> Unit = {}, onError: (String) -> Unit)
    fun connect(onError: (Core.Model.Error) -> Unit)
    fun disconnect(onErrorModel: (Core.Model.Error) -> Unit = {}, onError: (String) -> Unit)
    fun disconnect(onError: (Core.Model.Error) -> Unit)

    fun restart(onError: (Core.Model.Error) -> Unit)
}