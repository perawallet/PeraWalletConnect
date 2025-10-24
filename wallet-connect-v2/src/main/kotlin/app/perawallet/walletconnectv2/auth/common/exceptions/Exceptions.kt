

package app.perawallet.walletconnectv2.auth.common.exceptions

import app.perawallet.walletconnectv2.internal.common.exception.WalletConnectException

internal object MissingAuthRequestException : WalletConnectException(MISSING_AUTH_REQUEST_MESSAGE)
internal object InvalidCacaoException : WalletConnectException(CACAO_IS_NOT_VALID_MESSAGE)
internal class InvalidParamsException(override val message: String) : WalletConnectException(message)

class AuthClientAlreadyInitializedException : WalletConnectException(CLIENT_ALREADY_INITIALIZED)

class InvalidAuthParamsType : WalletConnectException(INVALID_AUTH_PARAMS_TYPE)