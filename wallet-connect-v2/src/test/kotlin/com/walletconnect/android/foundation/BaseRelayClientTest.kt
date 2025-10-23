package app.perawallet.walletconnectv2.foundation

import app.perawallet.walletconnectv2.foundation.common.model.SubscriptionId
import app.perawallet.walletconnectv2.foundation.network.BaseRelayClient
import app.perawallet.walletconnectv2.foundation.network.data.service.RelayService
import app.perawallet.walletconnectv2.foundation.network.model.Relay
import app.perawallet.walletconnectv2.foundation.network.model.RelayDTO
import app.perawallet.walletconnectv2.foundation.util.Logger
import app.perawallet.walletconnectv2.foundation.util.scope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BaseRelayClientTest {
	private val testDispatcher = StandardTestDispatcher()
	private val testScope = TestScope(testDispatcher)
	private lateinit var client: BaseRelayClient
	private val relayServiceMock = mockk<RelayService>(relaxed = true)
	private val loggerMock = mockk<Logger>(relaxed = true)

	@Before
	fun setup() {
		Dispatchers.setMain(testDispatcher)

		client = object : BaseRelayClient() {
			init {
				this.relayService = relayServiceMock
				this.logger = loggerMock
				scope = testScope
			}
		}
	}

	@After
	fun tearDown() {
		Dispatchers.resetMain()
	}

	@Test
	fun `test publish success`() = testScope.runTest {
		val topic = "testTopic"
		val message = "testMessage"
		val params = Relay.Model.IrnParams(1, 60, true)
		val ack = RelayDTO.Publish.Result.Acknowledgement(123L, result = true)

		coEvery { relayServiceMock.publishRequest(any()) } returns Unit
		coEvery { relayServiceMock.observePublishAcknowledgement() } returns flowOf(ack)

		client.observeResults()
		client.publish(topic, message, params, 123L) { result ->
			result.fold(
				onSuccess = {
					assertEquals(123L, it.id)
				},
				onFailure = { fail(it.message) }
			)
		}

		coVerify { relayServiceMock.publishRequest(any()) }
	}

	@Test
	fun `test publish error due to time out`() = testScope.runTest {
		val topic = "testTopic"
		val message = "testMessage"
		val params = Relay.Model.IrnParams(1, 60, true)

		coEvery { relayServiceMock.publishRequest(any()) } returns Unit
		coEvery { relayServiceMock.observePublishAcknowledgement() } returns flow { delay(10000L) }

		withContext(Dispatchers.Default.limitedParallelism(1)) {
			client.publish(topic, message, params) { result ->
				result.fold(
					onSuccess = {
						fail("Should not be successful")
					},
					onFailure = {
						assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
					}
				)
			}
		}

		advanceUntilIdle()

		coVerify { relayServiceMock.publishRequest(any()) }
	}

	@Test
	fun `test subscribe success`() = testScope.runTest {
		val topic = "testTopic"
		val expectedId = 123L
		val relayDto = RelayDTO.Subscribe.Result.Acknowledgement(id = expectedId, result = SubscriptionId("testId"))

		coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flowOf(relayDto)

		client.observeResults()
		client.subscribe(topic, expectedId) { result ->
			result.fold(
				onSuccess = {
					assertEquals(expectedId, result.getOrNull()?.id)
				},
				onFailure = { fail(it.message) }
			)
		}

		coVerify { relayServiceMock.subscribeRequest(any()) }
	}

	@Test
	fun `test subscribe failure due to timeout`() = testScope.runTest() {
		val topic = "testTopic"

		coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flow { delay(10000L) }

		client.subscribe(topic) { result ->
			result.fold(
				onSuccess = {
					fail("Should not be successful")
				},
				onFailure = {
					assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
				}
			)
		}

		testScheduler.apply { advanceTimeBy(5000); runCurrent() }

		coVerify { relayServiceMock.subscribeRequest(any()) }
	}

	@Test
	fun `test batch subscribe success`() = testScope.runTest {
		val topics = listOf("testTopic")
		val expectedId = 123L
		val relayDto = RelayDTO.BatchSubscribe.Result.Acknowledgement(id = expectedId, result = listOf("testId"))

		coEvery { relayServiceMock.batchSubscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeBatchSubscribeAcknowledgement() } returns flowOf(relayDto)

		client.observeResults()
		client.batchSubscribe(topics, expectedId) { result ->
			result.fold(
				onSuccess = {
					assertEquals(expectedId, result.getOrNull()?.id)
				},
				onFailure = { fail(it.message) }
			)
		}

		coVerify { relayServiceMock.batchSubscribeRequest(any()) }
	}

	@Test
	fun `test batch subscribe failure due to timeout`() = testScope.runTest {
		val topics = listOf("testTopic")

		coEvery { relayServiceMock.batchSubscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeBatchSubscribeAcknowledgement() } returns flow { delay(10000L) }

		client.batchSubscribe(topics) { result ->
			result.fold(
				onSuccess = {
					fail("Should not be successful")
				},
				onFailure = {
					assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
				}
			)

		}

		testScheduler.apply { advanceTimeBy(5000); runCurrent() }

		coVerify { relayServiceMock.batchSubscribeRequest(any()) }
	}

	@Test
	fun `test unsubscribe success`() = testScope.runTest {
		val topic = "testTopic"
		val expectedId = 123L
		val relayDto = RelayDTO.Unsubscribe.Result.Acknowledgement(id = expectedId, result = true)

		coEvery { relayServiceMock.unsubscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeUnsubscribeAcknowledgement() } returns flowOf(relayDto)

		client.observeResults()
		client.unsubscribe(topic, "subsId", expectedId) { result ->
			result.fold(
				onSuccess = {
					assertEquals(expectedId, result.getOrNull()?.id)
				},
				onFailure = { fail(it.message) }
			)
		}

		coVerify { relayServiceMock.unsubscribeRequest(any()) }
	}

	@Test
	fun `test unsubscribe failure`() = testScope.runTest {
		val topic = "testTopic"

		coEvery { relayServiceMock.subscribeRequest(any()) } returns Unit
		coEvery { relayServiceMock.observeSubscribeAcknowledgement() } returns flow { delay(10000L) }

		client.subscribe(topic) { result ->
			result.fold(
				onSuccess = {
					fail("Should not be successful")
				},
				onFailure = {
					assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
				}
			)
		}

		testScheduler.apply { advanceTimeBy(5000); runCurrent() }

		coVerify { relayServiceMock.subscribeRequest(any()) }
	}

}