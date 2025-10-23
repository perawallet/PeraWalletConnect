

package app.perawallet.walletconnectv2.internal.common.jwt.clientid

import android.content.SharedPreferences
import androidx.core.content.edit
import app.perawallet.walletconnectv2.internal.common.di.KEY_CLIENT_ID
import app.perawallet.walletconnectv2.utils.strippedUrl
import app.perawallet.walletconnectv2.foundation.crypto.data.repository.ClientIdJwtRepository

internal class GenerateJwtStoreClientIdUseCase(private val clientIdJwtRepository: ClientIdJwtRepository, private val sharedPreferences: SharedPreferences) {

    operator fun invoke(relayUrl: String): String =
        clientIdJwtRepository.generateJWT(relayUrl.strippedUrl()) { clientId ->
            sharedPreferences.edit {
                putString(KEY_CLIENT_ID, clientId)
            }
        }
}