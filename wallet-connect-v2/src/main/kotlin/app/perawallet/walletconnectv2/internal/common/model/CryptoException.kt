package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.common.exception.WalletConnectException

class UnknownEnvelopeTypeException(override val message: String?) : WalletConnectException(message)
class MissingParticipantsException(override val message: String?) : WalletConnectException(message)
class MissingKeyException(override val message: String?) : WalletConnectException(message)