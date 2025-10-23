package app.perawallet.walletconnectv2.sign.engine.use_case.requests

import app.perawallet.walletconnectv2.internal.common.model.IrnParams
import app.perawallet.walletconnectv2.internal.common.model.Tags
import app.perawallet.walletconnectv2.internal.common.model.WCRequest
import app.perawallet.walletconnectv2.internal.common.model.type.RelayJsonRpcInteractorInterface
import app.perawallet.walletconnectv2.internal.utils.thirtySeconds
import app.perawallet.walletconnectv2.foundation.common.model.Ttl
import app.perawallet.walletconnectv2.foundation.util.Logger
import kotlinx.coroutines.supervisorScope

internal class OnPingUseCase(private val jsonRpcInteractor: RelayJsonRpcInteractorInterface, private val logger: Logger) {

    suspend operator fun invoke(request: WCRequest) = supervisorScope {
        val irnParams = IrnParams(Tags.SESSION_PING_RESPONSE, Ttl(thirtySeconds))
        logger.log("Session ping received on topic: ${request.topic}")
        jsonRpcInteractor.respondWithSuccess(request, irnParams)
    }
}