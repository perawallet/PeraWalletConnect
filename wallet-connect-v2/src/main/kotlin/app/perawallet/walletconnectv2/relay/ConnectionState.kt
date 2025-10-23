@file:JvmSynthetic

package app.perawallet.walletconnectv2.relay

sealed class WSSConnectionState {
	object Connected : WSSConnectionState()

	sealed class Disconnected : WSSConnectionState() {
		data class ConnectionFailed(val throwable: Throwable) : Disconnected()
		data class ConnectionClosed(val message: String? = null) : Disconnected()
	}
}
