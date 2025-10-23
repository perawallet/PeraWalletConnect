package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.Core
import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.SDKError
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.utils.toClient
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.PeerError
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.params.SignParams
import app.perawallet.walletconnectv2.sign.common.model.vo.sequence.SessionVO
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toPeerError
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionApproved
import app.perawallet.walletconnectv2.sign.storage.proposal.ProposalStorageRepository
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import app.perawallet.walletconnectv2.internal.utils.Empty
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.supervisorScope

internal class OnSessionSettleUseCase(
    private val crypto: KeyManagementRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val sessionStorageRepository: SessionStorageRepository,
    private val pairingController: PairingControllerInterface,
    private val selfAppMetaData: AppMetaData,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val logger: Logger
) {
    private val _events: MutableSharedFlow<EngineEvent> = MutableSharedFlow()
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    suspend operator fun invoke(request: WCRequest, settleParams: SignParams.SessionSettleParams) = supervisorScope {
        logger.log("Session settle received on topic: ${request.topic}")
        val sessionTopic = request.topic
        val irnParams = IrnParams(Tags.SESSION_SETTLE_RESPONSE, Ttl(fiveMinutesInSeconds))
        val selfPublicKey: PublicKey = try {
            crypto.getSelfPublicFromKeyAgreement(sessionTopic)
        } catch (e: Exception) {
            logger.error("Session settle received failure: ${request.topic}, error: $e")
            jsonRpcInteractor.respondWithError(request, PeerError.Failure.SessionSettlementFailed(e.message ?: String.Empty), irnParams)
            return@supervisorScope
        }

        val peerMetadata = settleParams.controller.metadata
        val proposal = try {
            proposalStorageRepository.getProposalByKey(selfPublicKey.keyAsHex).also { proposalStorageRepository.deleteProposal(selfPublicKey.keyAsHex) }
        } catch (e: Exception) {
            logger.error("Session settle received failure: ${request.topic}, error: $e")
            jsonRpcInteractor.respondWithError(request, PeerError.Failure.SessionSettlementFailed(e.message ?: String.Empty), irnParams)
            return@supervisorScope
        }

        val (requiredNamespaces, optionalNamespaces, properties) = proposal.run { Triple(requiredNamespaces, optionalNamespaces, properties) }
        SignValidator.validateSessionNamespace(settleParams.namespaces, requiredNamespaces) { error ->
            logger.error("Session settle received failure - namespace validation: ${request.topic}, error: $error")
            jsonRpcInteractor.respondWithError(request, error.toPeerError(), irnParams)
            return@supervisorScope
        }

        try {
            val session = SessionVO.createAcknowledgedSession(
                sessionTopic,
                settleParams,
                selfPublicKey,
                selfAppMetaData,
                requiredNamespaces,
                optionalNamespaces,
                properties,
                proposal.pairingTopic.value
            )

            sessionStorageRepository.insertSession(session, request.id)
            pairingController.updateMetadata(Core.Params.UpdateMetadata(proposal.pairingTopic.value, peerMetadata.toClient(), AppMetaDataType.PEER))
            metadataStorageRepository.insertOrAbortMetadata(sessionTopic, peerMetadata, AppMetaDataType.PEER)
            jsonRpcInteractor.respondWithSuccess(request, irnParams)
            logger.log("Session settle received on topic: ${request.topic} - emitting")
            _events.emit(session.toSessionApproved())
        } catch (e: Exception) {
            logger.error("Session settle received failure: ${request.topic}, error: $e")
            proposalStorageRepository.insertProposal(proposal)
            sessionStorageRepository.deleteSession(sessionTopic)
            jsonRpcInteractor.respondWithError(request, PeerError.Failure.SessionSettlementFailed(e.message ?: String.Empty), irnParams)
            _events.emit(SDKError(e))
            return@supervisorScope
        }
    }
}