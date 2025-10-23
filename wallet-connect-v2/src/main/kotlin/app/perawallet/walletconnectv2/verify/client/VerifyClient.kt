package app.perawallet.walletconnectv2.verify.client

import app.perawallet.walletconnectv2.internal.common.di.verifyModule
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.pairing.handler.PairingControllerInterface
import app.perawallet.walletconnectv2.verify.domain.VerifyRepository
import app.perawallet.walletconnectv2.verify.domain.VerifyResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication

internal class VerifyClient(
    private val koinApp: KoinApplication = wcKoinApp,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : VerifyInterface {
    private val verifyRepository by lazy { koinApp.koin.get<VerifyRepository>() }
    private val pairingController: PairingControllerInterface by lazy { koinApp.koin.get() }

    override fun initialize() {
        koinApp.modules(verifyModule())

        scope.launch {
            pairingController.checkVerifyKeyFlow.collect {
                verifyRepository.getVerifyPublicKey().onFailure { throwable -> println("Error fetching a key: ${throwable.message}") }
            }
        }
    }

    override fun resolve(attestationId: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit) {
        try {
            verifyRepository.resolve(attestationId, metadataUrl, onSuccess, onError)
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun resolveV2(attestationId: String, attestationJWT: String, metadataUrl: String, onSuccess: (VerifyResult) -> Unit, onError: (Throwable) -> Unit) {
        try {
            verifyRepository.resolveV2(attestationId, attestationJWT, metadataUrl, onSuccess, onError)
        } catch (e: Exception) {
            onError(e)
        }
    }

    override fun register(attestationId: String, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        TODO("Not yet implemented")
    }
}