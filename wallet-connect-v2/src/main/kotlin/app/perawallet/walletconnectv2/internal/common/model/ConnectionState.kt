package app.perawallet.walletconnectv2.internal.common.model

import app.perawallet.walletconnectv2.internal.common.model.type.EngineEvent

data class ConnectionState(val isAvailable: Boolean, val reason: Reason? = null) : EngineEvent {
	sealed class Reason {
		data class ConnectionClosed(val message: String) : Reason()
		data class ConnectionFailed(val throwable: Throwable) : Reason()
	}
}
