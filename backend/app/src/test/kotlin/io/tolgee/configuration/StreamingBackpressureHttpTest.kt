package io.tolgee.configuration

import io.tolgee.Metrics
import io.tolgee.constants.Message
import io.tolgee.fixtures.waitForNotThrowing
import io.tolgee.testing.ContextRecreatingTest
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * MockMvc cannot answer this: it never performs the ASYNC dispatch that Spring's
 * WebAsyncManager triggers when the task executor rejects, so a MockMvc test sees a staged 200 and
 * an unset async result whether or not the 503 actually reaches a client. Only a real container
 * proves the rejection becomes a response.
 */
@ContextRecreatingTest
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = [
    "tolgee.async.streaming.max-threads = 1",
    "tolgee.async.streaming.queue-capacity = 0",
    "tolgee.internal.controller-enabled = true",
    "tolgee.rate-limits.global-limits = false",
    "tolgee.rate-limits.endpoint-limits = false",
    "tolgee.rate-limits.authentication-limits = false",
  ],
)
class StreamingBackpressureHttpTest {
  @LocalServerPort
  var port: Int = 0

  @Autowired
  @Qualifier(AsyncWebMvcConfiguration.STREAMING_EXECUTOR_BEAN_NAME)
  lateinit var streamingAsyncExecutor: ThreadPoolTaskExecutor

  @Autowired
  lateinit var metrics: Metrics

  private val release = CountDownLatch(1)
  private val client: HttpClient = HttpClient.newHttpClient()

  @AfterEach
  fun releasePool() {
    release.countDown()
    // Counting the latch down does not order the worker's return to the pool before the next test's
    // request arrives, and at queue-capacity 0 there is no slack to absorb that.
    awaitDrained()
  }

  @Test
  fun `streams normally while the pool has capacity`() {
    val response = get()

    response.statusCode().assert.isEqualTo(HttpStatus.OK.value())
    response.body().assert.isEqualTo("streamed")
  }

  @Test
  fun `answers 503 with Retry-After once the pool and its queue are full`() {
    occupyTheOnlyStreamingThread()
    val rejectedBefore = metrics.streamingRejectedCounter.count()

    val response = get()

    metrics.streamingRejectedCounter
      .count()
      .assert
      .isEqualTo(rejectedBefore + 1)

    response.statusCode().assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value())
    errorCodeOf(response.body()).assert.isEqualTo(Message.SERVER_BUSY.code)
    response
      .headers()
      .firstValue("Retry-After")
      .orElse(null)
      .assert
      .isEqualTo("5")
  }

  @Test
  fun `security headers survive on both a streamed response and a rejected one`() {
    val streamed = get().headers()
    streamed
      .firstValue("X-Content-Type-Options")
      .orElse(null)
      .assert
      .isEqualTo("nosniff")
    streamed
      .firstValue("X-Frame-Options")
      .orElse(null)
      .assert
      .isEqualTo("DENY")
    streamed
      .firstValue("Content-Disposition")
      .isPresent.assert
      .isTrue()
    streamed
      .firstValue("ETag")
      .isPresent.assert
      .isTrue()

    awaitDrained()
    occupyTheOnlyStreamingThread()

    val rejected = get()
    rejected.statusCode().assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value())
    rejected
      .headers()
      .firstValue("X-Content-Type-Options")
      .orElse(null)
      .assert
      .isEqualTo("nosniff")
    rejected
      .headers()
      .firstValue("X-Frame-Options")
      .orElse(null)
      .assert
      .isEqualTo("DENY")
  }

  @Test
  fun `the rejected response does not carry the staged streaming headers`() {
    occupyTheOnlyStreamingThread()

    val headers = get().headers()

    headers
      .firstValue("Content-Disposition")
      .isPresent.assert
      .isFalse()
    headers
      .firstValue("ETag")
      .isPresent.assert
      .isFalse()
  }

  private fun awaitDrained() {
    waitForNotThrowing(pollTime = 20, timeout = 10000) {
      streamingAsyncExecutor.activeCount.assert.isEqualTo(0)
    }
  }

  private fun occupyTheOnlyStreamingThread() {
    val occupied = CountDownLatch(1)
    streamingAsyncExecutor.execute {
      occupied.countDown()
      release.await(30, TimeUnit.SECONDS)
    }
    occupied.await(10, TimeUnit.SECONDS).assert.isTrue()
  }

  /** A real HTTP client, so the MockMvc andAssertThatJson helper is not available here. */
  private fun errorCodeOf(body: String): String? = ObjectMapper().readTree(body).get("code")?.asString()

  private fun get(): HttpResponse<String> {
    val request =
      HttpRequest
        .newBuilder(URI.create("http://localhost:$port/internal/streaming/stream"))
        .timeout(java.time.Duration.ofSeconds(20))
        .GET()
        .build()
    return client.send(request, HttpResponse.BodyHandlers.ofString())
  }
}
