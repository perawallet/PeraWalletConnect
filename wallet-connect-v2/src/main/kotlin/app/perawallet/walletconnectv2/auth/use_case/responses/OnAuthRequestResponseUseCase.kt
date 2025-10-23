package app.perawallet.walletconnectv2.auth.use_case.responses

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.WCResponse
import app.perawallet.walletconnectv2.internal.common.model.params.CoreAuthParams
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.signing.cacao.Cacao
import app.perawallet.walletconnectv2.internal.common.signing.cacao.CacaoVerifier
import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.utils.toClient
import app.perawallet.walletconnectv2.auth.common.exceptions.PeerError
import app.perawallet.walletconnectv2.auth.common.json_rpc.AuthParams
import app.perawallet.walletconnectv2.auth.common.model.AuthResponse
import app.perawallet.walletconnectv2.auth.common.model.Events
import app.perawallet.walletconnectv2.auth.engine.pairingTopicToResponseTopicMap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnAuthRequestResponseUseCase(
    private val pairingInterface: PairingInterface,
    private val pairingHandler: PairingControllerInterface,
    private val cacaoVerifier: CacaoVerifier,
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse, requestParams: AuthParams.RequestParams) = supervisorScope {
        try {
            val pairingTopic = wcResponse.topic
            if (!pairingInterface.getPairings().any { pairing -> pairing.topic == pairingTopic.value }) return@supervisorScope
            pairingTopicToResponseTopicMap.remove(pairingTopic)

            when (val response = wcResponse.response) {
                is JsonRpcResponse.JsonRpcError -> _events.emit(Events.OnAuthResponse(response.id, AuthResponse.Error(response.error.code, response.error.message)))
                is JsonRpcResponse.JsonRpcResult -> {
                    pairingHandler.updateMetadata(Core.Params.UpdateMetadata(pairingTopic.value, requestParams.requester.metadata.toClient(), AppMetaDataType.PEER))
                    val (header, payload, signature) = (response.result as CoreAuthParams.ResponseParams)
                    val cacao = Cacao(header, payload, signature)
                    if (cacaoVerifier.verify(cacao)) {
                        _events.emit(Events.OnAuthResponse(response.id, AuthResponse.Result(cacao)))
                    } else {
                        _events.emit(Events.OnAuthResponse(response.id, AuthResponse.Error(PeerError.SignatureVerificationFailed.code, PeerError.SignatureVerificationFailed.message)))
                    }
                }
            }
        } catch (e: Exception) {
            _events.emit(SDKError(e))
        }
    }
}