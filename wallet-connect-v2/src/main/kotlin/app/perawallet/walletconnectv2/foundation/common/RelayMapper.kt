

package app.perawallet.walletconnectv2.foundation.common

import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.WebSocket
import app.perawallet.walletconnectv2.foundation.network.model.Relay
import app.perawallet.walletconnectv2.foundation.network.model.RelayDTO


fun WebSocket.Event.toRelayEvent() = when (this) {
    is WebSocket.Event.OnConnectionOpened<*> ->
        Relay.Model.Event.OnConnectionOpened(webSocket)

    is WebSocket.Event.OnMessageReceived ->
        Relay.Model.Event.OnMessageReceived(message.toRelay())

    is WebSocket.Event.OnConnectionClosing ->
        Relay.Model.Event.OnConnectionClosing(shutdownReason.toRelay())

    is WebSocket.Event.OnConnectionClosed ->
        Relay.Model.Event.OnConnectionClosed(shutdownReason.toRelay())

    is WebSocket.Event.OnConnectionFailed ->
        Relay.Model.Event.OnConnectionFailed(throwable)
}


internal fun Message.toRelay() = when (this) {
    is Message.Text -> Relay.Model.Message.Text(value)
    is Message.Bytes -> Relay.Model.Message.Bytes(value)
}


internal fun ShutdownReason.toRelay() =
    Relay.Model.ShutdownReason(code, reason)


internal fun RelayDTO.Subscription.Request.Params.SubscriptionData.toRelay() =
    Relay.Model.Call.Subscription.Request.Params.SubscriptionData(topic.value, message, publishedAt, attestation, tag)


internal fun RelayDTO.Subscription.Request.Params.toRelay() =
    Relay.Model.Call.Subscription.Request.Params(subscriptionId.id, subscriptionData.toRelay())


fun RelayDTO.Subscription.Request.toRelay() =
    Relay.Model.Call.Subscription.Request(id, jsonrpc, method, params.toRelay())


fun RelayDTO.Publish.Result.Acknowledgement.toRelay() =
    Relay.Model.Call.Publish.Acknowledgement(id, jsonrpc, result)


fun RelayDTO.Subscribe.Result.Acknowledgement.toRelay() =
    Relay.Model.Call.Subscribe.Acknowledgement(id, jsonrpc, result.id)


fun RelayDTO.BatchSubscribe.Result.Acknowledgement.toRelay() =
    Relay.Model.Call.BatchSubscribe.Acknowledgement(id, jsonrpc, result)


fun RelayDTO.Unsubscribe.Result.Acknowledgement.toRelay() =
    Relay.Model.Call.Unsubscribe.Acknowledgement(id, jsonrpc, result)