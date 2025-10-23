package app.perawallet.walletconnectv2.pulse.domain

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.scope
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.internal.utils.currentTimeInSeconds
import app.perawallet.walletconnectv2.pulse.data.PulseService
import app.perawallet.walletconnectv2.pulse.model.Event
import app.perawallet.walletconnectv2.pulse.model.SDKType
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.foundation.util.generateId
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.qualifier.named

class SendEventUseCase(
    private val pulseService: PulseService,
    private val logger: Logger,
    private val bundleId: String,
) : SendEventInterface {
    private val enableW3MAnalytics: Boolean by lazy { wcKoinApp.koin.get(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) }

    override fun send(props: Props, sdkType: SDKType, timestamp: Long?, id: Long?) {
        if (enableW3MAnalytics) {
            scope.launch {
                supervisorScope {
                    try {
                        val event = Event(props = props, bundleId = bundleId, timestamp = timestamp ?: currentTimeInSeconds, eventId = id ?: generateId())
                        val response = pulseService.sendEvent(body = event, sdkType = sdkType.type)
                        if (!response.isSuccessful) {
                            logger.error("Failed to send event: ${event.props.type}")
                        } else {
                            logger.log("Event sent successfully: ${event.props.type}")
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to send event: ${props.type}, error: $e")
                    }
                }
            }
        }
    }
}

interface SendEventInterface {
    fun send(props: Props, sdkType: SDKType = SDKType.WEB3MODAL, timestamp: Long? = currentTimeInSeconds, id: Long? = generateId())
}