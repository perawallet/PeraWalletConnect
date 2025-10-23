package app.perawallet.walletconnectv2.pulse.domain

import app.perawallet.walletconnectv2.internal.common.model.TelemetryEnabled
import app.perawallet.walletconnectv2.internal.common.storage.events.EventsRepository
import app.perawallet.walletconnectv2.pulse.data.PulseService
import app.perawallet.walletconnectv2.pulse.model.Event
import app.perawallet.walletconnectv2.pulse.model.SDKType
import app.perawallet.walletconnectv2.foundation.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendBatchEventUseCase(
    private val pulseService: PulseService,
    private val eventsRepository: EventsRepository,
    private val telemetryEnabled: TelemetryEnabled,
    private val logger: Logger,
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        if (telemetryEnabled.value) {
            sendEventsInBatches { eventsRepository.getAllEventsWithLimitAndOffset(LIMIT, 0) }
        } else {
            try {
                eventsRepository.deleteAllTelemetry()
            } catch (e: Exception) {
                logger.error("Failed to delete events, error: $e")
            }

            sendEventsInBatches { eventsRepository.getAllNonTelemetryEventsWithLimitAndOffset(LIMIT, 0) }
        }
    }

    private suspend fun sendEventsInBatches(getEvents: suspend () -> List<Event>) {
        var continueProcessing = true
        while (continueProcessing) {
            val events = getEvents()
            if (events.isNotEmpty()) {
                try {
                    logger.log("Sending batch events: ${events.size}")
                    val response = pulseService.sendEventBatch(body = events, sdkType = SDKType.EVENTS.type)
                    if (response.isSuccessful) {
                        eventsRepository.deleteByIds(events.map { it.eventId })
                    } else {
                        logger.log("Failed to send events: ${events.size}")
                        continueProcessing = false
                    }
                } catch (e: Exception) {
                    logger.error("Error sending batch events: ${e.message}")
                    continueProcessing = false
                }
            } else {
                continueProcessing = false
            }
        }
    }

    companion object {
        private const val LIMIT = 500
    }
}