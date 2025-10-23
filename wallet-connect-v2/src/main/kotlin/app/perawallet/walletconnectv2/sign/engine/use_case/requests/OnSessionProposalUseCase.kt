package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.exception.Uncategorized
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.TransportType
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.pulse.domain.InsertTelemetryEventUseCase
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.verify.domain.ResolveAttestationIdUseCase
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toPeerError
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toVO
import app.perawallet.walletconnectv2.sign.json_rpc.model.JsonRpcMethod
import app.perawallet.walletconnectv2.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named

internal class OnSessionProposalUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val resolveAttestationIdUseCase: ResolveAttestationIdUseCase,
    private val pairingController: PairingControllerInterface,
    private val insertEventUseCase: InsertTelemetryEventUseCase,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()
    private val isAuthenticateEnabled: Boolean by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.ENABLE_AUTHENTICATE)) }

    suspend operator fun invoke(request: WCRequest, payloadParams: SignParams.SessionProposeParams) = supervisorScope {
        val irnParams = IrnParams(Tags.SESSION_PROPOSE_RESPONSE_AUTO_REJECT, Ttl(fiveMinutesInSeconds))
        try {
            if (isSessionAuthenticateImplemented(request)) {
                logger.error("Session proposal received error: pairing supports authenticated sessions")
                return@supervisorScope
            }
            logger.log("Session proposal received: ${request.topic}")
            SignValidator.validateProposalNamespaces(payloadParams.requiredNamespaces) { error ->
                logger.error("Session proposal received error: required namespace validation: ${error.message}")
                insertEventUseCase(Props(type = EventType.Error.REQUIRED_NAMESPACE_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            SignValidator.validateProposalNamespaces(payloadParams.optionalNamespaces ?: emptyMap()) { error ->
                logger.error("Session proposal received error: optional namespace validation: ${error.message}")
                insertEventUseCase(Props(type = EventType.Error.OPTIONAL_NAMESPACE_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                return@supervisorScope
            }

            payloadParams.properties?.let {
                SignValidator.validateProperties(payloadParams.properties) { error ->
                    logger.error("Session proposal received error: session properties validation: ${error.message}")
                    insertEventUseCase(Props(type = EventType.Error.SESSION_PROPERTIES_VALIDATION_FAILURE, properties = Properties(topic = request.topic.value)))
                    jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
                    return@supervisorScope
                }
            }
            proposalStorageRepository.insertProposal(payloadParams.toVO(request.topic, request.id))
            pairingController.setRequestReceived(Core.Params.RequestReceived(request.topic.value))
            val url = payloadParams.proposer.metadata.url

            logger.log("Resolving session proposal attestation: ${System.currentTimeMillis()}")
            resolveAttestationIdUseCase(request, url, linkMode = request.transportType == TransportType.LINK_MODE, appLink = payloadParams.proposer.metadata.redirect?.universal) { verifyContext ->
                logger.log("Session proposal attestation resolved: ${System.currentTimeMillis()}")
                val sessionProposalEvent = EngineDO.SessionProposalEvent(proposal = payloadParams.toEngineDO(request.topic), context = verifyContext.toEngineDO())
                logger.log("Session proposal received on topic: ${request.topic} - emitting")
                scope.launch { _events.emit(sessionProposalEvent) }
            }
        } catch (e: Exception) {
            logger.error("Session proposal received error: $e")
            jsonRpcInteractor.respondWithError(
                request,
                Uncategorized.GenericError("Cannot handle a session proposal: ${e.message}, topic: ${request.topic}"),
                irnParams
            )
            _events.emit(SDKError(e))
        }
    }

    private fun isSessionAuthenticateImplemented(request: WCRequest): Boolean =
        pairingController.getPairingByTopic(request.topic)?.methods?.contains(JsonRpcMethod.WC_SESSION_AUTHENTICATE) == true && isAuthenticateEnabled
}