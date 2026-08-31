package io.tolgee.unit

import io.tolgee.ExceptionHandlers
import io.tolgee.constants.Message
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.hibernate.query.sqm.PathElementException
import org.hibernate.query.sqm.UnknownPathException
import org.junit.jupiter.api.Test
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest

class ExceptionHandlersTest {
  private val handlers = ExceptionHandlers()

  @Test
  fun `answers bad request when a query names a property that does not exist`() {
    val ex = InvalidDataAccessApiUsageException("wrapped", PathElementException("Could not resolve attribute 'nope'"))

    val response = handlers.handleInvalidDataAccessApiUsage(ex, sortedRequest())

    assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    assertThat(response.body?.code).isEqualTo(Message.UNKNOWN_SORT_PROPERTY.code)
  }

  @Test
  fun `answers bad request when the unresolvable path sits below the direct cause`() {
    val buried =
      InvalidDataAccessApiUsageException(
        "wrapped",
        IllegalStateException("in between", UnknownPathException("Could not resolve path 'name.nope'")),
      )

    assertThat(
      handlers.handleInvalidDataAccessApiUsage(buried, sortedRequest()).statusCode,
    ).isEqualTo(HttpStatus.BAD_REQUEST)
  }

  @Test
  fun `rethrows data access misuse that is not an unresolvable property`() {
    val ex = InvalidDataAccessApiUsageException("something else entirely")

    assertThatThrownBy { handlers.handleInvalidDataAccessApiUsage(ex, sortedRequest()) }.isSameAs(ex)
  }

  @Test
  fun `rethrows an unresolvable path when the client did not ask for a sort`() {
    val ex = InvalidDataAccessApiUsageException("wrapped", PathElementException("Could not resolve attribute 'nope'"))

    assertThatThrownBy {
      handlers.handleInvalidDataAccessApiUsage(ex, MockHttpServletRequest())
    }.isSameAs(ex)
  }

  private fun sortedRequest() = MockHttpServletRequest().apply { setParameter("sort", "name") }
}
