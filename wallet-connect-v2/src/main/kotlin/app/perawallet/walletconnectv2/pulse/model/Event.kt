package app.perawallet.walletconnectv2.pulse.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import app.perawallet.walletconnectv2.internal.utils.currentTimeInSeconds
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.util.generateId

@JsonClass(generateAdapter = true)
data class Event(
    @Json(name = "eventId")
    val eventId: Long = generateId(),
    @Json(name = "bundleId")
    val bundleId: String,
    @Json(name = "timestamp")
    val timestamp: Long = currentTimeInSeconds,
    @Json(name = "props")
    val props: Props
)