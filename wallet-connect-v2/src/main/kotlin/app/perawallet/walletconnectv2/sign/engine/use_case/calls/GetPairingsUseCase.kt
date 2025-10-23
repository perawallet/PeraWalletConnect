package app.perawallet.walletconnectv2.sign.engine.use_case.calls

import app.perawallet.walletconnectv2.pairing.client.PairingInterface
import app.perawallet.walletconnectv2.pairing.model.mapper.toPairing
import app.perawallet.walletconnectv2.sign.engine.model.EngineDO
import kotlinx.coroutines.supervisorScope

internal class GetPairingsUseCase(private val pairingInterface: PairingInterface) : GetPairingsUseCaseInterface {

    override suspend fun getListOfSettledPairings(): List<EngineDO.PairingSettle> = supervisorScope {
        return@supervisorScope pairingInterface.getPairings().map { pairing ->
            val mappedPairing = pairing.toPairing()
            EngineDO.PairingSettle(mappedPairing.topic, mappedPairing.peerAppMetaData)
        }
    }
}

internal interface GetPairingsUseCaseInterface {
    suspend fun getListOfSettledPairings(): List<EngineDO.PairingSettle>
}