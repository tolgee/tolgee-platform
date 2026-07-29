package io.tolgee

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.io.IOException
import java.io.UncheckedIOException

/**
 * A client disconnecting mid-response is not a server fault, so it must not reach Sentry as one. The handler tells
 * that case apart by the socket message, which no compiler check protects.
 */
class ExceptionHandlersTest {
  private val handlers = ExceptionHandlers()

  @Test
  fun `answers bad gateway when the client aborted the connection`() {
    val response = handlers.handleOtherExceptions(IOException("Broken pipe"))

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
  }

  @Test
  fun `answers bad gateway when the client abort is a root cause`() {
    val response = handlers.handleOtherExceptions(UncheckedIOException(IOException("Broken pipe")))

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
  }

  @Test
  fun `answers server error for any other failure`() {
    val response = handlers.handleOtherExceptions(IOException("Disk on fire"))

    assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    assertThat(response.body?.code).isEqualTo("unexpected_error_occurred")
  }
}
