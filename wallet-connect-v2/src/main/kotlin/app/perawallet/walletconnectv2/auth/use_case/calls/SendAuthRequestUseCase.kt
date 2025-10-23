package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.InvalidExpiryException
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.Expiry
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.internal.utils.dayInSeconds
import app.perawallet.walletconnectv2.internal.utils.getParticipantTag
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthRpc
import app.perawallet.walletconnectv2.auth.common.model.PayloadParams
import app.perawallet.walletconnectv2.auth.common.model.Requester
import app.perawallet.walletconnectv2.auth.engine.pairingTopicToResponseTopicMap
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.util.Date
import java.util.concurrent.TimeUnit

internal class SendAuthRequestUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val selfAppMetaData: AppMetaData,
    private val logger: Logger
) : SendAuthRequestUseCaseInterface {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override suspend fun request(payloadParams: PayloadParams, expiry: Expiry?, topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        val nowInSeconds = TimeUnit.SECONDS.convert(Date().time, TimeUnit.SECONDS)
        if (!CoreValidator.isExpiryWithinBounds(expiry)) {
            return@supervisorScope onFailure(InvalidExpiryException())
        }

        val responsePublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
        val responseTopic: Topic = crypto.getTopicFromKey(responsePublicKey)
        val authParams: AuthParams.RequestParams = AuthParams.RequestParams(Requester(responsePublicKey.keyAsHex, selfAppMetaData), payloadParams, expiry)
        val authRequest: AuthRpc.AuthRequest = AuthRpc.AuthRequest(params = authParams)
        val irnParamsTtl = getIrnParamsTtl(expiry, nowInSeconds)
        val irnParams = IrnParams(Tags.AUTH_REQUEST, irnParamsTtl, true)
        val pairingTopic = Topic(topic)
        val requestTtlInSeconds = expiry?.run { seconds - nowInSeconds } ?: dayInSeconds
        crypto.setKey(responsePublicKey, responseTopic.getParticipantTag())

        jsonRpcInteractor.publishJsonRpcRequest(pairingTopic, irnParams, authRequest,
            onSuccess = {
                try {
                    jsonRpcInteractor.subscribe(responseTopic) { error -> return@subscribe onFailure(error) }
                } catch (e: Exception) {
                    return@publishJsonRpcRequest onFailure(e)
                }

                pairingTopicToResponseTopicMap[pairingTopic] = responseTopic
                onSuccess()
                collectPeerResponse(requestTtlInSeconds, authRequest)
            },
            onFailure = { error ->
                logger.error("Failed to send a auth request: $error")
                onFailure(error)
            }
        )
    }

    private fun getIrnParamsTtl(expiry: Expiry?, nowInSeconds: Long) = expiry?.run {
        val defaultTtl = dayInSeconds
        val extractedTtl = seconds - nowInSeconds
        val newTtl = extractedTtl.takeIf { extractedTtl >= defaultTtl } ?: defaultTtl
        Ttl(newTtl)
    } ?: Ttl(dayInSeconds)

    private fun collectPeerResponse(requestTtlInSeconds: Long, authRequest: AuthRpc.AuthRequest) {
        scope.launch {
            try {
                withTimeout(TimeUnit.SECONDS.toMillis(requestTtlInSeconds)) {
                    jsonRpcInteractor.peerResponse
                        .filter { response -> response.response.id == authRequest.id }
                        .collect { cancel() }
                }
            } catch (e: TimeoutCancellationException) {
                _events.emit(SDKError(e))
            }
        }
    }
}

internal interface SendAuthRequestUseCaseInterface {
    val events: SharedFlow<EngineEvent>
    suspend fun request(payloadParams: PayloadParams, expiry: Expiry? = null, topic: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit)
}