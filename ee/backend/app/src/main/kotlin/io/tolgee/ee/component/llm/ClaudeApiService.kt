package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto
import io.tolgee.service.PromptService
import io.tolgee.util.Logging
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class ClaudeApiService : AbstractLLMApiService(), Logging {
  override fun translate(
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("anthropic-version", "2023-06-01")
    headers.set("x-api-key", config.apiKey)

    val inputMessages = params.messages.toMutableList()

    if (params.shouldOutputJson) {
      inputMessages.add(
        LLMParams.Companion.LlmMessage(
          LLMParams.Companion.LlmMessageType.TEXT,
          "Strictly return only valid json!",
        ),
      )
    }

    val messages = mutableListOf<RequestMessage>()

    inputMessages.forEach {
      if (it.type == LLMParams.Companion.LlmMessageType.TEXT && it.text != null) {
        messages.add(RequestMessage(role = "user", content = it.text!!))
      } else if (it.type == LLMParams.Companion.LlmMessageType.IMAGE && it.image != null) {
        messages.add(
          RequestMessage(
            role = "user",
            content =
              listOf(
                RequestMessageContent(
                  type = "image",
                  source =
                    RequestImage(
                      data = Base64.getEncoder().encodeToString(it.image),
                    ),
                ),
              ),
          ),
        )
      }
    }

    val requestBody =
      RequestBody(
        messages = messages,
        response_format =
          if (params.shouldOutputJson) {
            when (config.format) {
              "json_object" -> ResponseFormat(type = "json_object", json_schema = null)
              "json_schema" -> ResponseFormat()
              else -> null
            }
          } else {
            null
          },
        model = config.model,
      )

    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<ResponseBody> =
      restTemplate.exchange<ResponseBody>(
        "${config.apiUrl}/v1/messages",
        HttpMethod.POST,
        request,
      )

    return PromptService.Companion.PromptResult(
      response.body?.content?.first()?.text ?: throw RuntimeException(response.toString()),
      usage =
        response.body?.usage?.let {
          PromptResponseUsageDto(
            inputTokens = it.input_tokens,
            outputTokens = it.output_tokens,
          )
        },
    )
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    @Suppress("unused")
    class RequestBody(
      val max_tokens: Long = 1000,
      val stream: Boolean = false,
      @JsonInclude(JsonInclude.Include.NON_NULL) val response_format: ResponseFormat? = null,
      val messages: List<RequestMessage>,
      val model: String?,
      val temperature: Long? = 0,
    )

    class RequestMessage(
      val role: String,
      val content: Any,
    )

    class RequestMessageContent(
      val type: String,
      val source: RequestImage,
    )

    class RequestImage(
      val type: String = "base64",
      val media_type: String = "image/png",
      val data: String,
    )

    class ResponseFormat(
      val type: String = "json_schema",
      @JsonInclude(JsonInclude.Include.NON_NULL) val json_schema: Map<String, Any>? =
        mapOf(
          "name" to "simple_response",
          "schema" to
            mapOf(
              "type" to "object",
              "properties" to
                mapOf(
                  "output" to mapOf("type" to "string"),
                  "contextDescription" to mapOf("type" to "string"),
                ),
              "required" to listOf("output", "contextDescription"),
              "additionalProperties" to false,
            ),
          "strict" to true,
        ),
    )

    class ResponseBody(
      val content: List<ResponseMessage>,
      val usage: ResponseUsage,
    )

    class ResponseMessage(
      val text: String,
    )

    class ResponseUsage(
      val input_tokens: Long,
      val output_tokens: Long,
    )
  }
}
