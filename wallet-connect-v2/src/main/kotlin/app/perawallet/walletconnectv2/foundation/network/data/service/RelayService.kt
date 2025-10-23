package app.perawallet.walletconnectv2.foundation.network.data.service

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import app.perawallet.walletconnectv2.foundation.network.model.RelayDTO
import kotlinx.coroutines.flow.Flow

interface RelayService {

    @Receive
    fun observeWebSocketEvent(): Flow<WebSocket.Event>

    @Send
    fun publishRequest(publishRequest: RelayDTO.Publish.Request)

    @Receive
    fun observePublishAcknowledgement(): Flow<RelayDTO.Publish.Result.Acknowledgement>

    @Receive
    fun observePublishError(): Flow<RelayDTO.Publish.Result.JsonRpcError>

    @Send
    fun subscribeRequest(subscribeRequest: RelayDTO.Subscribe.Request)

    @Receive
    fun observeSubscribeAcknowledgement(): Flow<RelayDTO.Subscribe.Result.Acknowledgement>

    @Receive
    fun observeSubscribeError(): Flow<RelayDTO.Subscribe.Result.JsonRpcError>

    @Send
    fun batchSubscribeRequest(subscribeRequest: RelayDTO.BatchSubscribe.Request)

    @Receive
    fun observeBatchSubscribeAcknowledgement(): Flow<RelayDTO.BatchSubscribe.Result.Acknowledgement>

    @Receive
    fun observeBatchSubscribeError(): Flow<RelayDTO.BatchSubscribe.Result.JsonRpcError>

    @Receive
    fun observeSubscriptionRequest(): Flow<RelayDTO.Subscription.Request>

    @Send
    fun publishSubscriptionAcknowledgement(publishRequest: RelayDTO.Subscription.Result.Acknowledgement)

    @Send
    fun unsubscribeRequest(unsubscribeRequest: RelayDTO.Unsubscribe.Request)

    @Receive
    fun observeUnsubscribeAcknowledgement(): Flow<RelayDTO.Unsubscribe.Result.Acknowledgement>

    @Receive
    fun observeUnsubscribeError(): Flow<RelayDTO.Unsubscribe.Result.JsonRpcError>
}
