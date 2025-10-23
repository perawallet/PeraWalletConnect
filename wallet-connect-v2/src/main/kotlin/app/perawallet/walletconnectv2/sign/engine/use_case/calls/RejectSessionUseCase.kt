package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.storage.verify.VerifyContextStorageRepository
import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.internal.utils.fiveMinutesInSeconds
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.sign.common.exceptions.PeerError
import app.perawallet.walletconnectv2.sign.common.exceptions.SessionProposalExpiredException
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toSessionProposeRequest
import app.perawallet.walletconnectv2.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

internal class RejectSessionUseCase(
    private val verifyContextStorageRepository: VerifyContextStorageRepository,
    private val jsonRpcInteractor: RelayJsonRpcInteractorInterface,
    private val proposalStorageRepository: ProposalStorageRepository,
    private val logger: Logger
) : RejectSessionUseCaseInterface {

    override suspend fun reject(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit) = supervisorScope {
        val proposal = proposalStorageRepository.getProposalByKey(proposerPublicKey)
        proposal.expiry?.let {
            if (it.isExpired()) {
                logger.error("Proposal expired on reject, topic: ${proposal.pairingTopic.value}, id: ${proposal.requestId}")
                throw SessionProposalExpiredException("Session proposal expired")
            }
        }

        logger.log("Sending session rejection, topic: ${proposal.pairingTopic.value}")
        jsonRpcInteractor.respondWithError(
            proposal.toSessionProposeRequest(),
            PeerError.EIP1193.UserRejectedRequest(reason),
            IrnParams(Tags.SESSION_PROPOSE_RESPONSE_REJECT, Ttl(fiveMinutesInSeconds)),
            onSuccess = {
                logger.log("Session rejection sent successfully, topic: ${proposal.pairingTopic.value}")
                scope.launch {
                    proposalStorageRepository.deleteProposal(proposerPublicKey)
                    verifyContextStorageRepository.delete(proposal.requestId)
                }
                onSuccess()
            },
            onFailure = { error ->
                logger.error("Session rejection sent failure, topic: ${proposal.pairingTopic.value}. Error: $error")
                onFailure(error)
            })
    }
}

internal interface RejectSessionUseCaseInterface {
    suspend fun reject(proposerPublicKey: String, reason: String, onSuccess: () -> Unit, onFailure: (Throwable) -> Unit = {})
}