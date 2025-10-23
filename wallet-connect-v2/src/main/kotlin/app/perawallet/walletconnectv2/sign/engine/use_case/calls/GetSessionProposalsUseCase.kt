package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.utils.CoreValidator.isExpired
import app.perawallet.walletconnectv2.sign.common.model.vo.proposal.ProposalVO
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toEngineDO
import app.perawallet.walletconnectv2.sign.storage.proposal.ProposalStorageRepository
import kotlinx.coroutines.supervisorScope

internal class GetSessionProposalsUseCase(private val proposalStorageRepository: ProposalStorageRepository) : GetSessionProposalsUseCaseInterface {
    override suspend fun getSessionProposals(): List<EngineDO.SessionProposal> =
        supervisorScope {
            proposalStorageRepository.getProposals().filter { proposal -> proposal.expiry?.let { !it.isExpired() } ?: true }.map(ProposalVO::toEngineDO)
        }
}

internal interface GetSessionProposalsUseCaseInterface {
    suspend fun getSessionProposals(): List<EngineDO.SessionProposal>
}