package app.perawallet.walletconnectv2.auth.use_case.requests

import app.perawallet.walletconnectv2.internal.common.exception.Invalid
import app.perawallet.walletconnectv2.internal.common.exception.Uncategorized
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.verify.domain.ResolveAttestationIdUseCase
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.model.Events
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class OnAuthRequestUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val resolveAttestationIdUseCase: ResolveAttestationIdUseCase,
    private val pairingController: PairingControllerInterface,
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcRequest: WCRequest, authParams: AuthParams.RequestParams) = supervisorScope {
        val irnParams = IrnParams(Tags.AUTH_REQUEST_RESPONSE, Ttl(dayInSeconds))
        try {
            authParams.expiry?.let {
                if (it.isExpired()) {
                    jsonRpcInteractor.respondWithError(wcRequest, Invalid.RequestExpired, irnParams)
                    return@supervisorScope
                }
            }

            val url = authParams.requester.metadata.url
            resolveAttestationIdUseCase(wcRequest, url) { verifyContext ->
                scope.launch { _events.emit(Events.OnAuthRequest(wcRequest.id, wcRequest.topic.value, authParams.payloadParams, verifyContext)) }
            }
        } catch (e: Exception) {
            jsonRpcInteractor.respondWithError(wcRequest, Uncategorized.GenericError("Cannot handle a auth request: ${e.message}, topic: ${wcRequest.topic}"), irnParams)
            _events.emit(SDKError(e))
        }
    }
}