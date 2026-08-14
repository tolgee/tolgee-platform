package io.tolgee.ee.unit

import io.tolgee.dtos.PromptResult
import io.tolgee.ee.service.prompt.PromptResultParser
import io.tolgee.exceptions.LlmProviderNotReturnedJsonException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper

class PromptResultParserTest {
  private val objectMapper = jacksonObjectMapper()

  private fun parse(response: String) = PromptResultParser(PromptResult(response, null), objectMapper).parse()

  @Test
  fun `parses scalar output and contextDescription`() {
    val result = parse("""{"output": "Ahoj", "contextDescription": "greeting"}""")
    assertThat(result.output).isEqualTo("Ahoj")
    assertThat(result.contextDescription).isEqualTo("greeting")
  }

  @Test
  fun `leaves contextDescription null when absent`() {
    assertThat(parse("""{"output": "Ahoj"}""").contextDescription).isNull()
  }

  @Test
  fun `throws when output is missing`() {
    assertThatThrownBy { parse("""{"contextDescription": "greeting"}""") }
      .isInstanceOf(LlmProviderNotReturnedJsonException::class.java)
  }

  @Test
  fun `throws when output is an object`() {
    assertThatThrownBy { parse("""{"output": {"text": "Ahoj"}}""") }
      .isInstanceOf(LlmProviderNotReturnedJsonException::class.java)
  }

  @Test
  fun `throws when output is an array`() {
    assertThatThrownBy { parse("""{"output": ["Ahoj"]}""") }
      .isInstanceOf(LlmProviderNotReturnedJsonException::class.java)
  }

  @Test
  fun `throws when contextDescription is an object`() {
    assertThatThrownBy { parse("""{"output": "Ahoj", "contextDescription": {"a": 1}}""") }
      .isInstanceOf(LlmProviderNotReturnedJsonException::class.java)
  }

  @Test
  fun `throws when contextDescription is an array`() {
    assertThatThrownBy { parse("""{"output": "Ahoj", "contextDescription": ["a"]}""") }
      .isInstanceOf(LlmProviderNotReturnedJsonException::class.java)
  }
}
