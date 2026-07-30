package io.tolgee.unit

import io.tolgee.ExceptionHandlers
import io.tolgee.constants.Message
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.query.sqm.PathElementException
import org.junit.jupiter.api.Test
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import java.io.IOException
import java.io.UncheckedIOException

class ExceptionHandlersTest {
  private val handlers = ExceptionHandlers()

  @Test
  fun `answers bad request when a query names a property that does not exist`() {
    val ex = InvalidDataAccessApiUsageException("wrapped", PathElementException("Could not resolve attribute 'nope'"))

    val response = handlers.handleInvalidDataAccessApiUsage(ex)

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.code).isEqualTo(Message.UNKNOWN_SORT_PROPERTY.code)
  }

  @Test
  fun `rethrows data access misuse that is not an unresolvable property`() {
    val ex = InvalidDataAccessApiUsageException("something else entirely")

    assertThatThrownBy { handlers.handleInvalidDataAccessApiUsage(ex) }.isSameAs(ex)
  }

  /**
   * The client-abort branch is recognised by the socket's "Broken pipe" text, which no compiler check protects.
   */
  @Test
  fun `answers bad gateway when the client aborted the connection`() {
    val response = handlers.handleOtherExceptions(IOException("Broken pipe"))

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
  }

  @Test
  fun `answers bad gateway when the client abort is anywhere in the cause chain`() {
    val nested = UncheckedIOException(IOException("Broken pipe"))

    assertThat(handlers.handleOtherExceptions(nested).statusCode).isEqualTo(HttpStatus.BAD_GATEWAY)
    assertThat(handlers.handleOtherExceptions(IllegalStateException("outer", nested)).statusCode)
      .isEqualTo(HttpStatus.BAD_GATEWAY)
  }

  @Test
  fun `answers server error for any other failure`() {
    val response = handlers.handleOtherExceptions(IOException("Disk on fire"))

    assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    assertThat(response.body?.code).isEqualTo("unexpected_error_occurred")
  }
}
