package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Issuer
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import app.perawallet.walletconnectv2.sign.engine.model.mapper.toCAIP222Message
import kotlinx.coroutines.supervisorScope

internal class FormatAuthenticateMessageUseCase : FormatAuthenticateMessageUseCaseInterface {
    @Throws(Exception::class)
    override suspend fun formatMessage(payloadParams: EngineDO.PayloadParams, iss: String): String = supervisorScope {
        val issuer = Issuer(iss)
        if (!payloadParams.chains.contains(issuer.chainId)) throw Exception("Issuer chainId does not match with PayloadParams")
        payloadParams.chains.forEach { chainId -> if (!CoreValidator.isChainIdCAIP2Compliant(chainId)) throw Exception("Chains must be CAIP-2 compliant") }
        if (!CoreValidator.isChainIdCAIP2Compliant(issuer.chainId)) throw Exception("Issuer chainId is not CAIP-2 compliant")
        if (!CoreValidator.isAccountIdCAIP10Compliant(issuer.accountId)) throw Exception("Issuer address is not CAIP-10 compliant")

        return@supervisorScope payloadParams.toCAIP222Message(issuer, "Ethereum")
    }
}

internal interface FormatAuthenticateMessageUseCaseInterface {
    @Throws(Exception::class)
    suspend fun formatMessage(payloadParams: EngineDO.PayloadParams, iss: String): String
}