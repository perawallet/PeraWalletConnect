@file:JvmSynthetic

package app.perawallet.walletconnectv2.auth.client

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.model.ConnectionState
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.auth.client.mapper.toAuth
import app.perawallet.walletconnectv2.auth.client.mapper.toClient
import app.perawallet.walletconnectv2.auth.client.mapper.toClientAuthContext
import app.perawallet.walletconnectv2.auth.client.mapper.toClientAuthRequest
import app.perawallet.walletconnectv2.auth.client.mapper.toCommon
import app.perawallet.walletconnectv2.auth.common.model.Events
import app.perawallet.walletconnectv2.auth.di.engineModule
import app.perawallet.walletconnectv2.auth.di.jsonRpcModule
import app.perawallet.walletconnectv2.auth.engine.domain.AuthEngine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinApplication

internal class AuthProtocol(private val koinApp: KoinApplication = wcKoinApp) : AuthInterface {
    private lateinit var authEngine: AuthEngine

    companion object {
        val instance = AuthProtocol()
    }

    @Throws(IllegalStateException::class)
    override fun initialize(params: Auth.Params.Init, onSuccess: () -> Unit, onError: (Auth.Model.Error) -> Unit) {
        if (!::authEngine.isInitialized) {
            try {
                koinApp.modules(
                    jsonRpcModule(),
                    engineModule(),
                )

                authEngine = koinApp.koin.get()
                authEngine.setup()
                onSuccess()
            } catch (e: Exception) {
                onError(Auth.Model.Error(e))
            }
        } else {
            onError(Auth.Model.Error(IllegalStateException("AuthClient already initialized")))
        }
    }

    @Throws(IllegalStateException::class)
    override fun setRequesterDelegate(delegate: AuthInterface.RequesterDelegate) {
        checkEngineInitialization()
        authEngine.engineEvent.onEach { event ->
            when (event) {
                is ConnectionState -> delegate.onConnectionStateChange(event.toClient())
                is SDKError -> delegate.onError(event.toClient())
                is Events.OnAuthResponse -> delegate.onAuthResponse(event.toClient())
            }
        }.launchIn(scope)
    }

    @Throws(IllegalStateException::class)
    override fun setResponderDelegate(delegate: AuthInterface.ResponderDelegate) {
        checkEngineInitialization()
        authEngine.engineEvent.onEach { event ->
            when (event) {
                is ConnectionState -> delegate.onConnectionStateChange(event.toClient())
                is SDKError -> delegate.onError(event.toClient())
                is Events.OnAuthRequest -> delegate.onAuthRequest(event.toClientAuthRequest(), event.toClientAuthContext())
            }
        }.launchIn(scope)
    }

    @Throws(IllegalStateException::class)
    override fun request(params: Auth.Params.Request, onSuccess: () -> Unit, onError: (Auth.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                val expiry = params.expiry?.run { Expiry(this) }
                authEngine.request(params.toCommon(), expiry, params.topic,
                    onSuccess = onSuccess,
                    onFailure = { error -> onError(Auth.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Auth.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun respond(params: Auth.Params.Respond, onSuccess: (Auth.Params.Respond) -> Unit, onError: (Auth.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                authEngine.respond(params.toCommon(), { onSuccess(params) }, { error -> onError(Auth.Model.Error(error)) })
            } catch (error: Exception) {
                onError(Auth.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun formatMessage(params: Auth.Params.FormatMessage): String? {
        checkEngineInitialization()

        return try {
            runBlocking { authEngine.formatMessage(params.payloadParams.toCommon(), params.issuer) }
        } catch (error: Exception) {
            null
        }
    }

    override fun decryptMessage(params: Auth.Params.DecryptMessage, onSuccess: (Auth.Model.Message.AuthRequest) -> Unit, onError: (Auth.Model.Error) -> Unit) {
        checkEngineInitialization()

        scope.launch {
            try {
                authEngine.decryptNotification(
                    topic = params.topic,
                    message = params.encryptedMessage,
                    onSuccess = { message -> (message as? Core.Model.Message.AuthRequest)?.run { onSuccess(message.toAuth()) } },
                    onFailure = { error -> onError(Auth.Model.Error(error)) }
                )
            } catch (error: Exception) {
                onError(Auth.Model.Error(error))
            }
        }
    }

    @Throws(IllegalStateException::class)
    override fun getPendingRequest(): List<Auth.Model.PendingRequest> {
        checkEngineInitialization()

        return runBlocking { authEngine.getPendingRequests().toClient() }
    }

    @Throws(IllegalStateException::class)
    override fun getVerifyContext(id: Long): Auth.Model.VerifyContext? {
        checkEngineInitialization()
        return runBlocking { authEngine.getVerifyContext(id)?.toClient() }
    }

    @Throws(IllegalStateException::class)
    override fun getListOfVerifyContexts(): List<Auth.Model.VerifyContext> {
        checkEngineInitialization()
        return runBlocking { authEngine.getListOfVerifyContext().map { verifyContext -> verifyContext.toClient() } }
    }

    @Throws(IllegalStateException::class)
    private fun checkEngineInitialization() {
        check(::authEngine.isInitialized) {
            "AuthClient needs to be initialized first using the initialize function"
        }
    }
}