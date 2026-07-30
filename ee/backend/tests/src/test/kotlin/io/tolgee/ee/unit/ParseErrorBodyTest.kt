package io.tolgee.ee.unit

import io.tolgee.constants.Message
import io.tolgee.ee.service.eeSubscription.parseTolgeeErrorBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException

class ParseErrorBodyTest {
  @Test
  fun `reads the error code`() {
    assertThat(badRequest("""{"code": "${Message.OUT_OF_CREDITS.code}"}""")?.code)
      .isEqualTo(Message.OUT_OF_CREDITS.code)
  }

  @Test
  fun `reads the error code when the remote sends fields we do not know`() {
    val body = """{"code": "${Message.OUT_OF_CREDITS.code}", "somethingAddedLater": {"a": 1}}"""

    assertThat(badRequest(body)?.code).isEqualTo(Message.OUT_OF_CREDITS.code)
  }

  @Test
  fun `reads the params the server sends alongside the code`() {
    val body = """{"code": "${Message.PLAN_KEY_LIMIT_EXCEEDED.code}", "params": ["100", "120"]}"""

    val parsed = badRequest(body)

    assertThat(parsed?.code).isEqualTo(Message.PLAN_KEY_LIMIT_EXCEEDED.code)
    assertThat(parsed?.params).containsExactly("100", "120")
  }

  @Test
  fun `keeps params it does not recognise rather than losing the code`() {
    val body = """{"code": "${Message.OUT_OF_CREDITS.code}", "params": [{"nested": 1}]}"""

    val parsed = badRequest(body)

    assertThat(parsed?.code).isEqualTo(Message.OUT_OF_CREDITS.code)
    assertThat(parsed?.params).hasSize(1)
  }

  @Test
  fun `returns null when the body has no textual code`() {
    assertThat(badRequest("""{"params": ["a"]}""")).isNull()
    assertThat(badRequest("""{"code": 42}""")).isNull()
  }

  @Test
  fun `returns null for a body that is not our error json`() {
    assertThat(badRequest("<html>502 Bad Gateway</html>")).isNull()
  }

  @Test
  fun `returns null for an empty body`() {
    assertThat(badRequest("")).isNull()
  }

  private fun badRequest(body: String) =
    HttpClientErrorException
      .create(
        HttpStatus.BAD_REQUEST,
        HttpStatus.BAD_REQUEST.reasonPhrase,
        HttpHeaders(),
        body.toByteArray(),
        null,
      ).parseTolgeeErrorBody()
}
