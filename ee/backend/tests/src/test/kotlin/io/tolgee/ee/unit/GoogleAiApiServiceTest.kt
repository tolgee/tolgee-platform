package io.tolgee.ee.unit

import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.ee.component.llm.GoogleAiApiService
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.model.enums.LlmProviderPriority
import io.tolgee.model.enums.LlmProviderType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoogleAiApiServiceTest {
  private lateinit var service: GoogleAiApiService

  @BeforeEach
  fun setUp() {
    service = GoogleAiApiService()
  }

  @Test
  fun `parses response omitting candidatesTokenCount`() {
    val restTemplate =
      stubLlmRestTemplate(
        """
        {
          "candidates": [{"content": {"role": "model", "parts": [{"text": "Ahoj svet"}]}, "finishReason": "STOP"}],
          "usageMetadata": {"promptTokenCount": 10, "totalTokenCount": 10}
        }
        """.trimIndent(),
      )

    val result = service.translate(createParams(), createConfig(), restTemplate)

    assertThat(result.response).isEqualTo("Ahoj svet")
    assertThat(result.usage?.inputTokens).isEqualTo(10)
    assertThat(result.usage?.outputTokens).isEqualTo(0)
  }

  @Test
  fun `throws empty response when the part carries no text`() {
    val restTemplate =
      stubLlmRestTemplate(
        """{"candidates": [{"content": {"parts": [{}]}}], "usageMetadata": {"promptTokenCount": 10}}""",
      )

    assertThatThrownBy { service.translate(createParams(), createConfig(), restTemplate) }
      .isInstanceOf(LlmEmptyResponseException::class.java)
  }

  private fun createConfig(): LlmProviderInterface {
    return object : LlmProviderInterface {
      override var name = "test-google"
      override var type = LlmProviderType.GOOGLE_AI
      override var priority: LlmProviderPriority? = LlmProviderPriority.HIGH
      override var apiKey: String? = "test-key"
      override var apiUrl: String? = "https://generativelanguage.googleapis.com"
      override var model: String? = "gemini-2.0-flash"
      override var format: String? = null
      override var deployment: String? = null
      override var reasoningEffort: String? = null
      override var maxTokens: Long = 1000
      override var tokenPriceInCreditsInput: Double? = null
      override var tokenPriceInCreditsOutput: Double? = null
      override var attempts: List<Int>? = null
    }
  }

  private fun createParams(): LlmParams {
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
    )
  }
}
