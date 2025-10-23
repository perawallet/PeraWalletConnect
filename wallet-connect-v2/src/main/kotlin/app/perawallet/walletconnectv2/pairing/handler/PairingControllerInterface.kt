package app.perawallet.walletconnectv2.pairing.handler

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.model.Pairing
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface PairingControllerInterface {
    val findWrongMethodsFlow: Flow<SDKError>
    val storedPairingFlow: SharedFlow<Pair<Topic, MutableList<String>>>
    val checkVerifyKeyFlow: SharedFlow<Unit>

    fun initialize()

    fun setRequestReceived(activate: Core.Params.RequestReceived, onError: (Core.Model.Error) -> Unit = {})

    fun updateMetadata(updateMetadata: Core.Params.UpdateMetadata, onError: (Core.Model.Error) -> Unit = {})

    fun deleteAndUnsubscribePairing(deletePairing: Core.Params.Delete, onError: (Core.Model.Error) -> Unit = {})

    fun register(vararg method: String)

    fun getPairingByTopic(topic: Topic): Pairing?
}