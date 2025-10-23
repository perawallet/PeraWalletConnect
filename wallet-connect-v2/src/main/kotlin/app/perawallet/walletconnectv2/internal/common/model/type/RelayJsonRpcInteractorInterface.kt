package app.perawallet.walletconnectv2.internal.common.model.type

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.model.EnvelopeType
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Participants
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.relay.WSSConnectionState
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import kotlinx.coroutines.flow.StateFlow

interface RelayJsonRpcInteractorInterface : JsonRpcInteractorInterface {
    val wssConnectionState: StateFlow<WSSConnectionState>
    fun checkConnectionWorking()

    fun subscribe(topic: Topic, onSuccess: (Topic) -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun batchSubscribe(topics: List<String>, onSuccess: (List<String>) -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun unsubscribe(topic: Topic, onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {})

    fun publishJsonRpcRequest(
        topic: Topic,
        params: IrnParams,
        payload: JsonRpcClientSync<*>,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )

    fun publishJsonRpcResponse(
        topic: Topic,
        params: IrnParams,
        response: JsonRpcResponse,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
        participants: Participants? = null,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
    )

    fun respondWithParams(
        request: WCRequest,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit = {}
    )

    fun respondWithParams(
        requestId: Long,
        topic: Topic,
        clientParams: ClientParams,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onFailure: (Throwable) -> Unit,
        onSuccess: () -> Unit = {}
    )

    fun respondWithSuccess(
        request: WCRequest,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
    )

    fun respondWithError(
        request: WCRequest,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: (WCRequest) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )

    fun respondWithError(
        requestId: Long,
        topic: Topic,
        error: Error,
        irnParams: IrnParams,
        envelopeType: EnvelopeType = EnvelopeType.ZERO,
        participants: Participants? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )
}