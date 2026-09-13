package io.tolgee

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.component.VersionFilter
import io.tolgee.constants.Message
import io.tolgee.exceptions.StreamingCapacityExceededException
import io.tolgee.exceptions.StreamingUnavailableException
import io.tolgee.testing.assert
import io.tolgee.util.StreamingResponseBodyProvider
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.core.task.TaskRejectedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import java.util.concurrent.RejectedExecutionException

class ExceptionHandlersAsyncCapacityTest {
  private val metrics = Metrics(SimpleMeterRegistry())
  private val exceptionHandlers = ExceptionHandlers(metrics, mock())

  @Test
  fun `answers a saturated streaming pool with 503 and Retry-After`() {
    val result = handle(MockHttpServletResponse())

    result.statusCode.assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    result.headers
      .getFirst(HttpHeaders.RETRY_AFTER)
      .assert
      .isEqualTo("5")
    result.body
      ?.code.assert
      .isEqualTo(Message.SERVER_BUSY.code)
  }

  @Test
  fun `drops the streaming headers already staged on the response`() {
    val response = MockHttpServletResponse()
    response.status = HttpStatus.OK.value()
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.zip")
    response.setHeader(HttpHeaders.ETAG, "\"some-etag\"")

    handle(response)

    response.getHeader(HttpHeaders.CONTENT_DISPOSITION).assert.isNull()
    response.getHeader(HttpHeaders.ETAG).assert.isNull()
  }

  @Test
  fun `keeps the CORS and version headers written by earlier filters`() {
    val response = MockHttpServletResponse()
    response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.tolgee.io")
    response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
    response.setHeader(HttpHeaders.VARY, "Origin")
    response.setHeader(VersionFilter.TOLGEE_VERSION_HEADER_NAME, "3.216.2")
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.zip")

    handle(response)

    response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN).assert.isEqualTo("https://app.tolgee.io")
    response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS).assert.isEqualTo("true")
    response.getHeader(HttpHeaders.VARY).assert.isEqualTo("Origin")
    response.getHeader(VersionFilter.TOLGEE_VERSION_HEADER_NAME).assert.isEqualTo("3.216.2")
    response.getHeader(HttpHeaders.CONTENT_DISPOSITION).assert.isNull()
  }

  @Test
  fun `leaves an already committed response alone`() {
    val response = MockHttpServletResponse()
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.zip")
    response.flushBuffer()

    handle(response).statusCode.assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    response.getHeader(HttpHeaders.CONTENT_DISPOSITION).assert.isNotNull
  }

  @Test
  fun `sends a rejection from another executor down the generic path`() {
    val unrelated = TaskRejectedException("some other executor is shutting down")

    val result = exceptionHandlers.handleAsyncCapacityExceeded(unrelated, MockHttpServletResponse())

    result.statusCode.assert.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    result.body
      ?.code.assert
      .isEqualTo("unexpected_error_occurred")
  }

  @Test
  fun `answers a request that aged out of the queue as capacity`() {
    val response = MockHttpServletResponse()
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=export.zip")

    val result =
      exceptionHandlers.handleAsyncRequestTimeout(
        AsyncRequestTimeoutException(),
        MockHttpServletRequest(),
        response,
      )

    result.statusCode.assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    result.body
      ?.code.assert
      .isEqualTo(Message.SERVER_BUSY.code)
    response.getHeader(HttpHeaders.CONTENT_DISPOSITION).assert.isNull()
  }

  /** A slow stream writes far less than one 8KB buffer, so commitment is not the signal. */
  @Test
  fun `reports a stream that ran and still timed out, even with nothing flushed`() {
    val request = MockHttpServletRequest()
    request.setAttribute(StreamingResponseBodyProvider.STREAM_STARTED_ATTRIBUTE, true)
    val response = MockHttpServletResponse()

    val result =
      exceptionHandlers.handleAsyncRequestTimeout(AsyncRequestTimeoutException(), request, response)

    response.isCommitted.assert.isFalse()
    result.statusCode.assert.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `counts a request that aged out of the queue`() {
    val before = metrics.streamingQueueTimeoutCounter.count()

    exceptionHandlers.handleAsyncRequestTimeout(
      AsyncRequestTimeoutException(),
      MockHttpServletRequest(),
      MockHttpServletResponse(),
    )

    metrics.streamingQueueTimeoutCounter
      .count()
      .assert
      .isEqualTo(before + 1)
  }

  /** A pool that is shutting down is unavailable, not overloaded — 503, but not a saturation count. */
  @Test
  fun `answers a shutdown rejection with 503 as well`() {
    val shuttingDown =
      TaskRejectedException("shutting down", StreamingUnavailableException("Streaming pool is shutting down"))

    val result = exceptionHandlers.handleAsyncCapacityExceeded(shuttingDown, MockHttpServletResponse())

    result.statusCode.assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
  }

  @Test
  fun `preserves every value of a repeated header it keeps`() {
    val response = MockHttpServletResponse()
    response.addHeader(HttpHeaders.VARY, "Origin")
    response.addHeader(HttpHeaders.VARY, "Accept-Encoding")
    response.setHeader(HttpHeaders.ETAG, "\"staged\"")

    handle(response)

    response.getHeaders(HttpHeaders.VARY).assert.containsExactly("Origin", "Accept-Encoding")
    response.getHeader(HttpHeaders.ETAG).assert.isNull()
  }

  private fun handle(response: MockHttpServletResponse) =
    exceptionHandlers.handleAsyncCapacityExceeded(wrappedRejection(), response)

  private fun wrappedRejection(): RejectedExecutionException =
    TaskRejectedException("pool full", StreamingCapacityExceededException("queued=1, active=1"))
}
