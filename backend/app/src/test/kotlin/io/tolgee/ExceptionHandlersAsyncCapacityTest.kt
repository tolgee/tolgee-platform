package io.tolgee

import io.tolgee.component.VersionFilter
import io.tolgee.constants.Message
import io.tolgee.exceptions.StreamingCapacityExceededException
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.springframework.core.task.TaskRejectedException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.async.AsyncRequestTimeoutException
import java.util.concurrent.RejectedExecutionException

class ExceptionHandlersAsyncCapacityTest {
  private val exceptionHandlers = ExceptionHandlers()

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

  /**
   * The streaming return-value handler stages these before the task is submitted; a caching proxy
   * would otherwise key the 503 against that ETag.
   */
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

  /** Without these the browser reports a CORS failure instead of surfacing the 503. */
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

  /** Any other executor's rejection is not ours to relabel as streaming saturation. */
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

    val result = exceptionHandlers.handleAsyncRequestTimeout(AsyncRequestTimeoutException(), response)

    result.statusCode.assert.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
    result.body
      ?.code.assert
      .isEqualTo(Message.SERVER_BUSY.code)
    response.getHeader(HttpHeaders.CONTENT_DISPOSITION).assert.isNull()
  }

  /** A stream that timed out mid-write cannot be answered, and is worth reporting rather than hiding. */
  @Test
  fun `reports a stream that timed out after it started writing`() {
    val response = MockHttpServletResponse()
    response.flushBuffer()

    val result = exceptionHandlers.handleAsyncRequestTimeout(AsyncRequestTimeoutException(), response)

    result.statusCode.assert.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
  }

  private fun handle(response: MockHttpServletResponse) =
    exceptionHandlers.handleAsyncCapacityExceeded(wrappedRejection(), response)

  /** Spring wraps whatever the rejection policy throws, so the handler only ever sees the wrapper. */
  private fun wrappedRejection(): RejectedExecutionException =
    TaskRejectedException("pool full", StreamingCapacityExceededException("queued=1, active=1"))
}
