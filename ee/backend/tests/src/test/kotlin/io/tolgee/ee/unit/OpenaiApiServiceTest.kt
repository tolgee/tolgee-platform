package io.tolgee.ee.unit

import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.LlmResponseSchema
import io.tolgee.ee.component.llm.OpenaiApiService
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class OpenaiApiServiceTest {
  private lateinit var service: OpenaiApiService

  @BeforeEach
  fun setUp() {
    service = OpenaiApiService()
  }

  @Test
  fun `parses response omitting prediction token details`() {
    val restTemplate =
      stubLlmRestTemplate(
        """
        {
          "id": "gen-123",
          "provider": "OpenRouter",
          "model": "openai/gpt-4o",
          "object": "chat.completion",
          "choices": [{"index": 0, "finish_reason": "stop", "message": {"role": "assistant", "content": "Ahoj svet"}}],
          "usage": {
            "prompt_tokens": 10,
            "completion_tokens": 5,
            "total_tokens": 15,
            "prompt_tokens_details": {"cached_tokens": 2},
            "completion_tokens_details": {"reasoning_tokens": 0}
          }
        }
        """.trimIndent(),
      )

    val result = service.translate(createParams(), createConfig(), restTemplate)

    assertThat(result.response).isEqualTo("Ahoj svet")
    assertThat(result.usage?.inputTokens).isEqualTo(10)
    assertThat(result.usage?.outputTokens).isEqualTo(5)
    assertThat(result.usage?.cachedTokens).isEqualTo(2)
  }

  @Test
  fun `parses response omitting token details objects entirely`() {
    val restTemplate =
      stubLlmRestTemplate(
        """
        {
          "choices": [{"message": {"role": "assistant", "content": "Ahoj svet"}}],
          "usage": {"prompt_tokens": 10, "completion_tokens": 5}
        }
        """.trimIndent(),
      )

    val result = service.translate(createParams(), createConfig(), restTemplate)

    assertThat(result.response).isEqualTo("Ahoj svet")
    assertThat(result.usage?.inputTokens).isEqualTo(10)
    assertThat(result.usage?.cachedTokens).isNull()
  }

  @Test
  fun `throws empty response when the message carries no content`() {
    val restTemplate =
      stubLlmRestTemplate(
        """{"choices": [{"message": {"role": "assistant"}}], "usage": {"prompt_tokens": 10}}""",
      )

    assertThatThrownBy { service.translate(createParams(), createConfig(), restTemplate) }
      .isInstanceOf(LlmEmptyResponseException::class.java)
  }

  @Test
  fun `sends the response schema from params when json_schema format is configured`() {
    var body: String? = null
    val restTemplate = stubLlmRestTemplate(OK_RESPONSE) { body = it }

    service.translate(createParams(responseSchema = REVIEW_SCHEMA), createConfig(), restTemplate)

    val jsonSchema = responseFormatSchema(body!!)
    assertThat(jsonSchema["name"]).isEqualTo("review_response")
    assertThat(jsonSchema["schema"]).isEqualTo(REVIEW_SCHEMA.schema)
    assertThat(jsonSchema["strict"]).isEqualTo(true)
  }

  @Test
  fun `sends the translation schema when params have no response schema`() {
    var body: String? = null
    val restTemplate = stubLlmRestTemplate(OK_RESPONSE) { body = it }

    service.translate(createParams(), createConfig(), restTemplate)

    val jsonSchema = responseFormatSchema(body!!)
    assertThat(jsonSchema["name"]).isEqualTo("simple_response")
  }

  @Suppress("UNCHECKED_CAST")
  private fun responseFormatSchema(body: String): Map<String, Any> {
    val bodyMap = jacksonObjectMapper().readValue<Map<String, Any>>(body)
    val responseFormat = bodyMap["response_format"] as Map<String, Any>
    return responseFormat["json_schema"] as Map<String, Any>
  }

  private fun createConfig(): LlmProviderInterface {
    return object : LlmProviderInterface {
      override var name = "test-openai"
      override var type = LlmProviderType.OPENAI
      override var priority: LlmProviderPriority? = LlmProviderPriority.HIGH
      override var apiKey: String? = "test-key"
      override var apiUrl: String? = "https://openrouter.ai/api"
      override var model: String? = "openai/gpt-4o"
      override var format: String? = "json_schema"
      override var deployment: String? = null
      override var reasoningEffort: String? = null
      override var maxTokens: Long = 1000
      override var tokenPriceInCreditsInput: Double? = null
      override var tokenPriceInCreditsOutput: Double? = null
      override var attempts: List<Int>? = null
    }
  }

  private fun createParams(responseSchema: LlmResponseSchema? = null): LlmParams {
    return LlmParams(
      messages =
        listOf(
          LlmParams.Companion.LlmMessage(
            type = LlmParams.Companion.LlmMessageType.TEXT,
            text = "Translate 'hello' to Czech",
          ),
        ),
      shouldOutputJson = true,
      priority = LlmProviderPriority.HIGH,
      responseSchema = responseSchema,
    )
  }

  companion object {
    private const val OK_RESPONSE =
      """{"choices": [{"message": {"role": "assistant", "content": "{}"}}], "usage": {"prompt_tokens": 1, "completion_tokens": 1}}"""

    val REVIEW_SCHEMA =
      LlmResponseSchema(
        name = "review_response",
        schema = mapOf("type" to "object", "properties" to mapOf("issues" to mapOf("type" to "array"))),
      )
  }
}
