package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.crypto.kmr.KeyManagementRepository
import app.perawallet.walletconnectv2.internal.common.exception.NoInternetConnectionException
import app.perawallet.walletconnectv2.internal.common.exception.NoRelayConnectionException
import app.perawallet.walletconnectv2.internal.common.model.AppMetaData
import app.perawallet.walletconnectv2.internal.common.model.AppMetaDataType
import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.metadata.MetadataStorageRepositoryInterface
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.internal.utils.ACTIVE_SESSION
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.pulse.domain.InsertTelemetryEventUseCase
import app.perawallet.walletconnectv2.pulse.model.EventType
import app.perawallet.walletconnectv2.pulse.model.Trace
import app.perawallet.walletconnectv2.pulse.model.properties.Properties
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.common.model.PublicKey
import app.perawallet.walletconnectv2.foundation.common.model.Topic
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.InvalidNamespaceException
import app.perawallet.walletconnectv2.sign.common.exceptions.SessionProposalExpiredException
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.common.SessionParticipant
import app.perawallet.walletconnectv2.sign.common.model.vo.clientsync.session.SignRpc
import app.perawallet.walletconnectv2.sign.common.model.vo.proposal.ProposalVO
import app.perawallet.walletconnectv2.sign.common.model.vo.sequence.SessionVO
import app.perawallet.walletconnectv2.sign.common.validator.SignValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toMapOfNamespacesVOSession
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionApproveParams
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionProposeRequest
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionSettleParams
import app.perawallet.walletconnectv2.sign.storage.proposal.ProposalStorageRepository
import app.perawallet.walletconnectv2.sign.storage.sequence.SessionStorageRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class ApproveSessionUseCase(
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val crypto: KeyManagementRepository,
    private val sessionStorageRepository: SessionStorageRepository,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val metadataStorageRepository: MetadataStorageRepositoryInterface,
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val selfAppMetaData: AppMetaData,
    private val insertEventUseCase: InsertTelemetryEventUseCase,
    private val logger: Logger
) : ApproveSessionUseCaseInterface {

    override suspend fun approve(
        proposerPublicKey: String,
        sessionNamespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) = supervisorScope {
        val trace: MutableList<String> = mutableListOf()
        trace.add(Trace.Session.SESSION_APPROVE_STARTED).also { logger.log(Trace.Session.SESSION_APPROVE_STARTED) }
        fun sessionSettle(requestId: Long, proposal: ProposalVO, sessionTopic: Topic, pairingTopic: Topic) {
            val selfPublicKey = crypto.getSelfPublicFromKeyAgreement(sessionTopic)
            val selfParticipant = SessionParticipant(selfPublicKey.keyAsHex, selfAppMetaData)
            val sessionExpiry = ACTIVE_SESSION
            val unacknowledgedSession = SessionVO.createUnacknowledgedSession(sessionTopic, proposal, selfParticipant, sessionExpiry, sessionNamespaces, pairingTopic.value)
            try {
                sessionStorageRepository.insertSession(unacknowledgedSession, requestId)
                metadataStorageRepository.insertOrAbortMetadata(sessionTopic, selfAppMetaData, AppMetaDataType.SELF)
                metadataStorageRepository.insertOrAbortMetadata(sessionTopic, proposal.appMetaData, AppMetaDataType.PEER)
                trace.add(Trace.Session.STORE_SESSION)
                val params = proposal.toSessionSettleParams(selfParticipant, sessionExpiry, sessionNamespaces)
                val sessionSettle = SignRpc.SessionSettle(params = params)
                val irnParams = IrnParams(Tags.SESSION_SETTLE, Ttl(fiveMinutesInSeconds))
                trace.add(Trace.Session.PUBLISHING_SESSION_SETTLE).also { logger.log("Publishing session settle on topic: $sessionTopic") }
                jsonRpcInteractor.publishJsonRpcRequest(
                    topic = sessionTopic,
                    params = irnParams, sessionSettle,
                    onSuccess = {
                        onSuccess()
                        scope.launch {
                            supervisorScope {
                                proposalStorageRepository.deleteProposal(proposerPublicKey)
                                verifyContextStorageRepository.delete(proposal.requestId)
                                trace.add(Trace.Session.SESSION_SETTLE_PUBLISH_SUCCESS).also { logger.log("Session settle sent successfully on topic: $sessionTopic") }
                            }
                        }
                    },
                    onFailure = { error ->
                        scope.launch {
                            supervisorScope {
                                insertEventUseCase(Props(type = EventType.Error.SESSION_SETTLE_PUBLISH_FAILURE, properties = Properties(trace = trace, topic = pairingTopic.value)))
                            }
                        }.also { logger.error("Session settle failure on topic: $sessionTopic, error: $error") }
                        onFailure(error)
                    }
                )
            } catch (e: Exception) {
                if (e is NoRelayConnectionException)
                    scope.launch {
                        supervisorScope {
                            insertEventUseCase(Props(type = EventType.Error.NO_WSS_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic.value)))
                        }
                    }
                if (e is NoInternetConnectionException)
                    scope.launch {
                        supervisorScope {
                            insertEventUseCase(Props(type = EventType.Error.NO_INTERNET_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic.value)))
                        }
                    }
                sessionStorageRepository.deleteSession(sessionTopic)
                logger.error("Session settle failure, error: $e")
                // todo: missing metadata deletion. Also check other try catches
                onFailure(e)
            }
        }

        val proposal = proposalStorageRepository.getProposalByKey(proposerPublicKey)
        val request = proposal.toSessionProposeRequest()
        val pairingTopic = proposal.pairingTopic.value
        try {
            proposal.expiry?.let {
                if (it.isExpired()) {
                    insertEventUseCase(Props(type = EventType.Error.PROPOSAL_EXPIRED, properties = Properties(trace = trace, topic = pairingTopic)))
                        .also { logger.error("Proposal expired on approve, topic: $pairingTopic, id: ${proposal.requestId}") }
                    throw SessionProposalExpiredException("Session proposal expired")
                }
            }
            trace.add(Trace.Session.PROPOSAL_NOT_EXPIRED)
            SignValidator.validateSessionNamespace(sessionNamespaces.toMapOfNamespacesVOSession(), proposal.requiredNamespaces) { error ->
                insertEventUseCase(Props(type = EventType.Error.SESSION_APPROVE_NAMESPACE_VALIDATION_FAILURE, properties = Properties(trace = trace, topic = pairingTopic)))
                    .also { logger.log("Session approve failure - invalid namespaces, error: $error") }
                throw InvalidNamespaceException(error.message)
            }
            trace.add(Trace.Session.SESSION_NAMESPACE_VALIDATION_SUCCESS)
            val selfPublicKey: PublicKey = crypto.generateAndStoreX25519KeyPair()
            val sessionTopic = crypto.generateTopicFromKeyAgreement(selfPublicKey, PublicKey(proposerPublicKey))
            trace.add(Trace.Session.CREATE_SESSION_TOPIC)
            val approvalParams = proposal.toSessionApproveParams(selfPublicKey)
            val irnParams = IrnParams(Tags.SESSION_PROPOSE_RESPONSE_APPROVE, Ttl(fiveMinutesInSeconds))
            trace.add(Trace.Session.SUBSCRIBING_SESSION_TOPIC).also { logger.log("Subscribing to session topic: $sessionTopic") }
            jsonRpcInteractor.subscribe(sessionTopic,
                onSuccess = {
                    trace.add(Trace.Session.SUBSCRIBE_SESSION_TOPIC_SUCCESS).also { logger.log("Successfully subscribed to session topic: $sessionTopic") }
                },
                onFailure = { error ->
                    scope.launch {
                        supervisorScope {
                            insertEventUseCase(Props(type = EventType.Error.SESSION_SUBSCRIPTION_FAILURE, properties = Properties(trace = trace, topic = pairingTopic)))
                        }
                    }.also { logger.error("Subscribe to session topic failure: $error") }
                    onFailure(error)
                })

            sessionSettle(request.id, proposal, sessionTopic, request.topic)

            trace.add(Trace.Session.PUBLISHING_SESSION_APPROVE).also { logger.log("Publishing session approve on topic: $sessionTopic") }
            jsonRpcInteractor.respondWithParams(request, approvalParams, irnParams,
                onSuccess = {
                    trace.add(Trace.Session.SESSION_APPROVE_PUBLISH_SUCCESS).also { logger.log("Session approve sent successfully, topic: $sessionTopic") }
                },
                onFailure = { error ->
                    scope.launch {
                        supervisorScope {
                            insertEventUseCase(Props(type = EventType.Error.SESSION_APPROVE_PUBLISH_FAILURE, properties = Properties(trace = trace, topic = pairingTopic)))
                        }
                    }.also { logger.error("Session approve failure, topic: $sessionTopic: $error") }
                    onFailure(error)
                })
        } catch (e: Exception) {
            if (e is NoRelayConnectionException) insertEventUseCase(Props(type = EventType.Error.NO_WSS_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic)))
            if (e is NoInternetConnectionException) insertEventUseCase(Props(type = EventType.Error.NO_INTERNET_CONNECTION, properties = Properties(trace = trace, topic = pairingTopic)))
            onFailure(e)
        }
    }
}

internal interface ApproveSessionUseCaseInterface {
    suspend fun approve(
        proposerPublicKey: String,
        sessionNamespaces: Map<String, EngineDO.Namespace.Session>,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    )
}