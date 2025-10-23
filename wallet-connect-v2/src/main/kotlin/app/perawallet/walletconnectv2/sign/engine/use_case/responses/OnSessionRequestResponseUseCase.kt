package app.perawallet.walletconnectv2.sign.engine.use_case.responses

import app.perawallet.walletconnectv2.internal.common.JsonRpcResponse
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.WCResponse
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.pulse.domain.InsertEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Direction
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.json_rpc.domain.GetSessionRequestByIdUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionRequestResponseUseCase(
    private val logger: Logger,
    private val insertEventUseCase: InsertEventUseCase,
    private val getSessionRequestByIdUseCase: GetSessionRequestByIdUseCase,
    private val clientId: String
) {

    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(wcResponse: WCResponse, params: SignParams.SessionRequestParams) = supervisorScope {
        try {
            val jsonRpcHistoryEntry = getSessionRequestByIdUseCase(wcResponse.response.id)
            logger.log("Session request response received on topic: ${wcResponse.topic}")
            val result = when (wcResponse.response) {
                is JsonRpcResponse.JsonRpcResult -> wcResponse.response.toEngineDO()
                is JsonRpcResponse.JsonRpcError -> wcResponse.response.toEngineDO()
            }

            if (jsonRpcHistoryEntry?.transportType == TransportType.LINK_MODE) {
                insertEventUseCase(
                    Props(
                        EventType.SUCCESS,
                        Tags.SESSION_REQUEST_LINK_MODE_RESPONSE.id.toString(),
                        Properties(correlationId = wcResponse.response.id, clientId = clientId, direction = Direction.RECEIVED.state)
                    )
                )
            }
            val method = params.request.method
            logger.log("Session request response received on topic: ${wcResponse.topic} - emitting: $result")
            _events.emit(EngineDO.SessionPayloadResponse(wcResponse.topic.value, params.chainId, method, result))
        } catch (e: Exception) {
            logger.error("Session request response received failure: $e")
            _events.emit(SDKError(e))
        }
    }
}