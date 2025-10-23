package app.perawallet.walletconnectv2.internal

import app.perawallet.walletconnectv2.internal.common.di.AndroidCommonDITags
import app.perawallet.walletconnectv2.internal.common.wcKoinApp
import app.perawallet.walletconnectv2.pulse.data.PulseService
import app.perawallet.walletconnectv2.pulse.domain.SendEventUseCase
import app.perawallet.walletconnectv2.pulse.model.Event
import app.perawallet.walletconnectv2.pulse.model.SDKType
import app.perawallet.walletconnectv2.pulse.model.properties.Props
import app.perawallet.walletconnectv2.foundation.util.Logger
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import retrofit2.Response

class SendEventUseCaseTest : KoinTest {
    private val pulseService: PulseService = mockk()
    private val logger: Logger = mockk()
    private val bundleId: String = "testBundleId"
    private lateinit var useCase: SendEventUseCase
    private val testDispatcher = StandardTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send should log and send event when analytics is enabled and response is successful`() = runTest(testDispatcher) {
        stopKoin()
        val module: Module = module {
            single(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) { true }
        }
        val app = startKoin { modules(module) }
        useCase = SendEventUseCase(pulseService, logger, bundleId)
        wcKoinApp = app

        val props = Props(type = "testEvent")
        val sdkType = SDKType.WEB3MODAL
        val event = Event(props = props, bundleId = bundleId)

        coEvery { pulseService.sendEvent(body = any(), sdkType = any()) } returns Response.success(Unit)
        every { logger.log("Event sent successfully: ${event.props.type}") } just Runs

        useCase.send(props, sdkType, event.timestamp, event.eventId)

        advanceUntilIdle()

        coVerify { pulseService.sendEvent(body = event, sdkType = sdkType.type) }
        verify { logger.log("Event sent successfully: ${event.props.type}") }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send should log error when analytics is enabled and response is unsuccessful`() = runTest(testDispatcher) {
        stopKoin()
        val module: Module = module {
            single(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) { true }
        }
        val app = startKoin { modules(module) }
        useCase = SendEventUseCase(pulseService, logger, bundleId)
        wcKoinApp = app

        val props = Props(type = "testEvent")
        val sdkType = SDKType.WEB3MODAL
        val event = Event(props = props, bundleId = bundleId)

        coEvery { pulseService.sendEvent(any(), any()) } returns Response.error(400, "ResponseBody".toResponseBody())
        every { logger.error("Failed to send event: ${event.props.type}") } just Runs

        useCase.send(props, sdkType, event.timestamp, event.eventId)

        advanceUntilIdle()

        coVerify { pulseService.sendEvent(body = event, sdkType = sdkType.type) }
        verify { logger.error("Failed to send event: ${event.props.type}") }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send should log exception when analytics is enabled and an exception occurs`() = runTest(testDispatcher) {
        stopKoin()
        val module: Module = module {
            single(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) { true }
        }
        val app = startKoin { modules(module) }
        useCase = SendEventUseCase(pulseService, logger, bundleId)
        wcKoinApp = app

        val props = Props(type = "testEvent")
        val sdkType = SDKType.WEB3MODAL
        val event = Event(props = props, bundleId = bundleId)
        val exception = Exception("Test exception")

        coEvery { pulseService.sendEvent(any(), any()) } throws exception
        every { logger.error("Failed to send event: ${props.type}, error: $exception") } just Runs

        useCase.send(props, sdkType, event.timestamp, event.eventId)

        advanceUntilIdle()

        coVerify { pulseService.sendEvent(body = event, sdkType = sdkType.type) }
        verify { logger.error("Failed to send event: ${props.type}, error: $exception") }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send should not send event when analytics is disabled`() = runTest(testDispatcher) {
        stopKoin()
        val module: Module = module {
            single(named(AndroidCommonDITags.ENABLE_WEB_3_MODAL_ANALYTICS)) { false }
        }
        val app2 = startKoin { modules(module) }
        useCase = SendEventUseCase(pulseService, logger, bundleId)
        wcKoinApp = app2

        val props = Props(type = "testEvent")
        val sdkType = SDKType.WEB3MODAL
        val event = Event(props = props, bundleId = bundleId)

        useCase.send(props, sdkType, event.timestamp, event.eventId)

        advanceUntilIdle()

        coVerify(exactly = 0) { pulseService.sendEvent(any(), any()) }
        verify(exactly = 0) { logger.log("") }
        verify(exactly = 0) { logger.error("") }
    }
}