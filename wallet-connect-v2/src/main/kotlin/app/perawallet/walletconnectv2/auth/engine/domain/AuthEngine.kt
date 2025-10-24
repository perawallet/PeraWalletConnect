

package app.perawallet.walletconnectv2.auth.engine.domain

import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Validation
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.push.notifications.DecryptMessageUseCaseInterface
import app.perawallet.walletconnectv2.relay.WSSConnectionState
import app.perawallet.walletconnectv2.verify.model.VerifyContext
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.model.Events
import app.perawallet.walletconnectv2.auth.engine.pairingTopicToResponseTopicMap
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntriesUseCaseInterface
import app.perawallet.walletconnectv2.auth.json_rpc.domain.GetPendingJsonRpcHistoryEntryByTopicUseCase
import app.perawallet.walletconnectv2.auth.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.auth.use_case.calls.FormatMessageUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.GetListOfVerifyContextsUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.GetVerifyContextUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.RespondAuthRequestUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.calls.SendAuthRequestUseCaseInterface
import app.perawallet.walletconnectv2.auth.use_case.requests.OnAuthRequestUseCase
import app.perawallet.walletconnectv2.auth.use_case.responses.OnAuthRequestResponseUseCase
import app.perawallet.walletconnectv2.internal.utils.Empty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class AuthEngine(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val getPendingJsonRpcHistoryEntriesUseCase: GetPendingJsonRpcHistoryEntriesUseCaseInterface,
    private val getPendingJsonRpcHistoryEntryByTopicUseCase: GetPendingJsonRpcHistoryEntryByTopicUseCase,
    private val pairingHandler: PairingControllerInterface,
    private val sendAuthRequestUseCase: SendAuthRequestUseCaseInterface,
    private val respondAuthRequestUseCase: RespondAuthRequestUseCaseInterface,
    private val formatMessageUseCase: FormatMessageUseCaseInterface,
    private val decryptAuthMessageUseCase: DecryptMessageUseCaseInterface,
    private val getVerifyContextUseCase: GetVerifyContextUseCaseInterface,
    private val getListOfVerifyContextsUseCase: GetListOfVerifyContextsUseCaseInterface,
    private val onAuthRequestUseCase: OnAuthRequestUseCase,
    private val onAuthRequestResponseUseCase: OnAuthRequestResponseUseCase,
) : SendAuthRequestUseCaseInterface by sendAuthRequestUseCase,
    RespondAuthRequestUseCaseInterface by respondAuthRequestUseCase,
    FormatMessageUseCaseInterface by formatMessageUseCase,
    DecryptMessageUseCaseInterface by decryptAuthMessageUseCase,
    GetPendingJsonRpcHistoryEntriesUseCaseInterface by getPendingJsonRpcHistoryEntriesUseCase,
    GetVerifyContextUseCaseInterface by getVerifyContextUseCase,
    GetListOfVerifyContextsUseCaseInterface by getListOfVerifyContextsUseCase {
    private var jsonRpcRequestsJob: Job? = null
    private var jsonRpcResponsesJob: Job? = null
    private var internalErrorsJob: Job? = null
    private var authEventsJob: Job? = null

    private val _engineEvent: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val engineEvent: SharedFlow<EngineEvent> = _engineEvent.asSharedFlow()

    init {
        pairingHandler.register(JsonRpcMethod.WC_AUTH_REQUEST)
        emitReceivedAuthRequest()
    }

    fun setup() {
        jsonRpcInteractor.wssConnectionState
            .filterIsInstance<WSSConnectionState.Connected>()
            .onEach {
                supervisorScope {
                    launch(Dispatchers.IO) { resubscribeToPendingRequestsTopics() }
                }

                if (jsonRpcRequestsJob == null) {
                    jsonRpcRequestsJob = collectJsonRpcRequests()
                }
                if (jsonRpcResponsesJob == null) {
                    jsonRpcResponsesJob = collectJsonRpcResponses()
                }
                if (internalErrorsJob == null) {
                    internalErrorsJob = collectInternalErrors()
                }
                if (authEventsJob == null) {
                    authEventsJob = collectAuthEvents()
                }
            }
            .launchIn(scope)
    }

    private fun collectJsonRpcRequests(): Job =
        jsonRpcInteractor.clientSyncJsonRpc
            .filter { request -> request.params is AuthParams.RequestParams }
            .onEach { request -> onAuthRequestUseCase(request, request.params as AuthParams.RequestParams) }
            .launchIn(scope)

    private fun collectJsonRpcResponses(): Job =
        jsonRpcInteractor.peerResponse
            .filter { response -> response.params is AuthParams }
            .onEach { response -> onAuthRequestResponseUseCase(response, response.params as AuthParams.RequestParams) }
            .launchIn(scope)

    private fun resubscribeToPendingRequestsTopics() {
        val responseTopics = pairingTopicToResponseTopicMap.map { (_, responseTopic) -> responseTopic.value }
        try {
            jsonRpcInteractor.batchSubscribe(responseTopics) { error -> scope.launch { _engineEvent.emit(SDKError(error)) } }
        } catch (e: Exception) {
            scope.launch { _engineEvent.emit(SDKError(e)) }
        }
    }

    private fun emitReceivedAuthRequest() {
        pairingHandler.storedPairingFlow
            .onEach { (pairingTopic, trace) ->
                try {
                    val request = getPendingJsonRpcHistoryEntryByTopicUseCase(pairingTopic)
                    val context = verifyContextStorageRepository.get(request.id) ?: VerifyContext(request.id, String.Empty, Validation.UNKNOWN, String.Empty, null)
                    scope.launch { _engineEvent.emit(Events.OnAuthRequest(request.id, request.pairingTopic, request.payloadParams, context)) }
                } catch (e: Exception) {
                    println("No auth request for pairing topic: $e")
                }
            }.launchIn(scope)
    }

    private fun collectAuthEvents(): Job =
        merge(sendAuthRequestUseCase.events, onAuthRequestUseCase.events, onAuthRequestResponseUseCase.events)
            .onEach { event -> _engineEvent.emit(event) }
            .launchIn(scope)

    private fun collectInternalErrors(): Job =
        merge(jsonRpcInteractor.internalErrors, pairingHandler.findWrongMethodsFlow)
            .onEach { exception -> _engineEvent.emit(exception) }
            .launchIn(scope)
}