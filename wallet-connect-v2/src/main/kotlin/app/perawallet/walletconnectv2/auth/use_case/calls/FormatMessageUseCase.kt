package app.perawallet.walletconnectv2.auth.use_case.calls

import app.perawallet.walletconnectv2.internal.common.signing.cacao.Issuer
import app.perawallet.walletconnectv2.internal.utils.CoreValidator
import app.perawallet.walletconnectv2.auth.common.exceptions.InvalidParamsException
import app.perawallet.walletconnectv2.auth.common.model.PayloadParams
import app.perawallet.walletconnectv2.auth.engine.mapper.toCAIP222Message
import kotlinx.coroutines.supervisorScope

internal class FormatMessageUseCase : FormatMessageUseCaseInterface {
    override suspend fun formatMessage(payloadParams: PayloadParams, iss: String): String = supervisorScope {
        val issuer = Issuer(iss)
        if (issuer.chainId != payloadParams.chainId) throw InvalidParamsException("Issuer chainId does not match with PayloadParams")
        if (!CoreValidator.isChainIdCAIP2Compliant(payloadParams.chainId)) throw InvalidParamsException("PayloadParams chainId is not CAIP-2 compliant")
        if (!CoreValidator.isChainIdCAIP2Compliant(issuer.chainId)) throw InvalidParamsException("Issuer chainId is not CAIP-2 compliant")
        if (!CoreValidator.isAccountIdCAIP10Compliant(issuer.accountId)) throw InvalidParamsException("Issuer address is not CAIP-10 compliant")

        return@supervisorScope payloadParams.toCAIP222Message(issuer)
    }
}

internal interface FormatMessageUseCaseInterface {
    suspend fun formatMessage(payloadParams: PayloadParams, iss: String): String
}