

package app.perawallet.walletconnectv2.relay

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.connection.ConnectivityState
import app.perawallet.walletconnectv2.internal.common.connection.ManualConnectionLifecycle
import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.exception.WRONG_CONNECTION_TYPE
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.utils.toWalletConnectException
import app.perawallet.walletconnectv2.foundation.network.BaseRelayClient
import app.perawallet.walletconnectv2.foundation.network.model.Relay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.KoinApplication
import org.koin.core.qualifier.named

class RelayClient(private val koinApp: KoinApplication = wcKoinApp) : BaseRelayClient(), RelayConnectionInterface {
    private val manualConnection: ManualConnectionLifecycle by lazy { koinApp.koin.get(named(AndroidCommonDITags.MANUAL_CONNECTION_LIFECYCLE)) }
    private val networkState: ConnectivityState by lazy { koinApp.koin.get(named(AndroidCommonDITags.CONNECTIVITY_STATE)) }
    override val isNetworkAvailable: StateFlow<Boolean?> by lazy { networkState.isAvailable }
    private val _wssConnectionState: MutableStateFlow<WSSConnectionState> = MutableStateFlow(WSSConnectionState.Disconnected.ConnectionClosed())
    override val wssConnectionState: StateFlow<WSSConnectionState> = _wssConnectionState
    private lateinit var connectionType: ConnectionType

    
    fun initialize(connectionType: ConnectionType, onError: (Throwable) -> Unit) {
        this.connectionType = connectionType
        logger = koinApp.koin.get(named(AndroidCommonDITags.LOGGER))
        relayService = koinApp.koin.get(named(AndroidCommonDITags.RELAY_SERVICE))
        collectConnectionInitializationErrors { error -> onError(error) }
        monitorConnectionState()
        observeResults()
    }

    private fun collectConnectionInitializationErrors(onError: (Throwable) -> Unit) {
        scope.launch {
            supervisorScope {
                eventsFlow
                    .takeWhile { event ->
                        if (event is Relay.Model.Event.OnConnectionFailed) {
                            onError(event.throwable.toWalletConnectException)
                        }

                        event !is Relay.Model.Event.OnConnectionOpened<*>
                    }.collect()
            }
        }
    }

    private fun monitorConnectionState() {
        eventsFlow
            .onEach { event: Relay.Model.Event -> setIsWSSConnectionOpened(event) }
            .launchIn(scope)
    }

    private fun setIsWSSConnectionOpened(event: Relay.Model.Event) {
        when {
            event is Relay.Model.Event.OnConnectionOpened<*> && _wssConnectionState.value is WSSConnectionState.Disconnected ->
                _wssConnectionState.value = WSSConnectionState.Connected

            event is Relay.Model.Event.OnConnectionFailed && _wssConnectionState.value is WSSConnectionState.Connected ->
                _wssConnectionState.value = WSSConnectionState.Disconnected.ConnectionFailed(event.throwable)

            event is Relay.Model.Event.OnConnectionClosed && _wssConnectionState.value is WSSConnectionState.Connected ->
                _wssConnectionState.value = WSSConnectionState.Disconnected.ConnectionClosed("Connection closed: ${event.shutdownReason.reason} ${event.shutdownReason.code}")
        }
    }

    override fun connect(onError: (Core.Model.Error) -> Unit) {
        when (connectionType) {
            ConnectionType.AUTOMATIC -> onError(Core.Model.Error(IllegalStateException(WRONG_CONNECTION_TYPE)))
            ConnectionType.MANUAL -> manualConnection.connect()
        }
    }

    override fun disconnect(onError: (Core.Model.Error) -> Unit) {
        when (connectionType) {
            ConnectionType.AUTOMATIC -> onError(Core.Model.Error(IllegalStateException(WRONG_CONNECTION_TYPE)))
            ConnectionType.MANUAL -> manualConnection.disconnect()
        }
    }

    override fun restart(onError: (Core.Model.Error) -> Unit) {
        try {
            when (connectionType) {
                ConnectionType.AUTOMATIC -> onError(Core.Model.Error(IllegalStateException(WRONG_CONNECTION_TYPE)))
                ConnectionType.MANUAL -> manualConnection.restart()
            }
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    override fun connect(onErrorModel: (Core.Model.Error) -> Unit, onError: (String) -> Unit) {
        when (connectionType) {
            ConnectionType.AUTOMATIC -> onError(WRONG_CONNECTION_TYPE)
            ConnectionType.MANUAL -> manualConnection.connect()
        }
    }

    override fun disconnect(onErrorModel: (Core.Model.Error) -> Unit, onError: (String) -> Unit) {
        when (connectionType) {
            ConnectionType.AUTOMATIC -> onError(WRONG_CONNECTION_TYPE)
            ConnectionType.MANUAL -> manualConnection.disconnect()
        }
    }
}