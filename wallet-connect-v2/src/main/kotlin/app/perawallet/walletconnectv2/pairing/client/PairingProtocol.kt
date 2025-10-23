@file:JvmSynthetic

package app.perawallet.walletconnectv2.pairing.client

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.Validator
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.pairing.engine.domain.PairingEngine
import app.perawallet.walletconnectv2.pairing.engine.model.EngineDO
import app.perawallet.walletconnectv2.pairing.model.mapper.toCore
import app.perawallet.walletconnectv2.pulse.domain.InsertTelemetryEventUseCase
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.relay.RelayConnectionInterface
import app.perawallet.walletconnectv2.relay.WSSConnectionState
import app.perawallet.walletconnectv2.foundation.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.koin.core.KoinApplication

internal class PairingProtocol(private val koinApp: KoinApplication = wcKoinApp) : PairingInterface {
    private lateinit var pairingEngine: PairingEngine
    private val logger: Logger by lazy { koinApp.koin.get() }
    private val relayClient: RelayConnectionInterface by lazy { koinApp.koin.get() }
    private val insertEventUseCase: InsertTelemetryEventUseCase by lazy { koinApp.koin.get() }

    override fun initialize() {
        pairingEngine = koinApp.koin.get()
    }

    override fun setDelegate(delegate: PairingInterface.Delegate) {
        checkEngineInitialization()

        pairingEngine.engineEvent.onEach { event ->
            when (event) {
                is EngineDO.PairingDelete -> delegate.onPairingDelete(event.toCore())
                is EngineDO.PairingExpire -> delegate.onPairingExpired(Core.Model.ExpiredPairing(event.pairing.toCore()))
                is EngineDO.PairingState -> delegate.onPairingState(Core.Model.PairingState(event.isPairingState))
            }
        }.launchIn(scope)
    }

    @Throws(IllegalStateException::class)
    override fun create(onError: (Core.Model.Error) -> Unit): Core.Model.Pairing? {
        checkEngineInitialization()

        return try {
            pairingEngine.create({ error -> onError(Core.Model.Error(error)) })
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
            null
        }
    }

    @Throws(IllegalStateException::class)
    override fun create(onError: (Core.Model.Error) -> Unit, methods: String): Core.Model.Pairing? {
        checkEngineInitialization()

        return try {
            pairingEngine.create({ error -> onError(Core.Model.Error(error)) }, methods)
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
            null
        }
    }

    @Throws(IllegalStateException::class)
    override fun pair(
        pair: Core.Params.Pair,
        onSuccess: (Core.Params.Pair) -> Unit,
        onError: (Core.Model.Error) -> Unit,
    ) {
        checkEngineInitialization()

        scope.launch(Dispatchers.IO) {
            awaitConnection(
                {
                    try {
                        pairingEngine.pair(
                            uri = pair.uri,
                            onSuccess = { onSuccess(pair) },
                            onFailure = { error -> onError(Core.Model.Error(error)) }
                        )
                    } catch (e: Exception) {
                        onError(Core.Model.Error(e))
                    }
                },
                { throwable ->
                    logger.error(throwable)
                    onError(Core.Model.Error(Throwable("Pairing error: ${throwable.message}")))
                })
        }
    }

    @Throws(IllegalStateException::class)
    override fun disconnect(disconnect: Core.Params.Disconnect, onError: (Core.Model.Error) -> Unit) {
        checkEngineInitialization()

        try {
            pairingEngine.disconnect(disconnect.topic) { error -> onError(Core.Model.Error(error)) }
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    @Throws(IllegalStateException::class)
    override fun disconnect(topic: String, onError: (Core.Model.Error) -> Unit) {
        checkEngineInitialization()

        try {
            pairingEngine.disconnect(topic) { error -> onError(Core.Model.Error(error)) }
        } catch (e: Exception) {
            onError(Core.Model.Error(e))
        }
    }

    @Throws(IllegalStateException::class)
    override fun ping(ping: Core.Params.Ping, pairingPing: Core.Listeners.PairingPing?) {
        checkEngineInitialization()

        try {
            pairingEngine.ping(ping.topic,
                onSuccess = { topic -> pairingPing?.onSuccess(Core.Model.Ping.Success(topic)) },
                onFailure = { error -> pairingPing?.onError(Core.Model.Ping.Error(error)) })
        } catch (e: Exception) {
            pairingPing?.onError(Core.Model.Ping.Error(e))
        }
    }

    @Throws(IllegalStateException::class)
    override fun getPairings(): List<Core.Model.Pairing> {
        checkEngineInitialization()

        return pairingEngine.getPairings().map { pairing -> pairing.toCore() }
    }

    override fun validatePairingUri(uri: String): Boolean {
        return try {
            Validator.validateWCUri(uri) != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun awaitConnection(onConnection: () -> Unit, errorLambda: (Throwable) -> Unit = {}) {
        try {
            withTimeout(60000) {
                while (true) {
                    if (relayClient.isNetworkAvailable.value != null) {
                        if (relayClient.isNetworkAvailable.value == true) {
                            if (relayClient.wssConnectionState.value is WSSConnectionState.Connected) {
                                onConnection()
                                return@withTimeout
                            }
                        } else {
                            insertEventUseCase(Props(type = EventType.Error.NO_INTERNET_CONNECTION))
                            errorLambda(Throwable("No internet connection"))
                            return@withTimeout
                        }
                    }
                    delay(100)
                }
            }
        } catch (e: Exception) {
            insertEventUseCase(Props(type = EventType.Error.NO_WSS_CONNECTION))
            errorLambda(Throwable("Failed to connect: ${e.message}"))
        }
    }

    @Throws(IllegalStateException::class)
    private fun checkEngineInitialization() {
        check(::pairingEngine.isInitialized) {
            "CoreClient needs to be initialized first using the initialize function"
        }
    }
}