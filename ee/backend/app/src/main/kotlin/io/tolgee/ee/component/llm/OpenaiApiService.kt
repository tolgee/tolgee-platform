package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiApiService : AbstractLLMApiService(), Logging {
  override fun defaultAttempts(): List<Int> = listOf(15, 30)

  override fun translate(
    params: LLMParams,
    config: LLMProviderInterface,
    restTemplate: RestTemplate,
  ): PromptService.Companion.PromptResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("api-key", config.apiKey)

    val inputMessages = params.messages.toMutableList()

    if (params.shouldOutputJson) {
      inputMessages.add(
        LLMParams.Companion.LlmMessage(LLMParams.Companion.LlmMessageType.TEXT, "Return only valid json!"),
      )
    }

    val messages = mutableListOf<RequestMessage>()

    inputMessages.forEach {
      if (
        it.type == LLMParams.Companion.LlmMessageType.TEXT &&
        it.text != null
      ) {
        messages.add(RequestMessage(role = "user", content = it.text!!))
      } else if (
        it.type == LLMParams.Companion.LlmMessageType.IMAGE &&
        it.image != null
      ) {
        messages.add(
          RequestMessage(
            role = "user",
            content =
              listOf(
                RequestMessageContent(
                  type = "image_url",
                  image_url =
                    RequestImageUrl(
                      "data:image/jpeg;base64,${
                        Base64.getEncoder().encodeToString(it.image)
                      }",
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
              "json_object" -> RequestResponseFormat(type = "json_object", json_schema = null)
              "json_schema" -> RequestResponseFormat()
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
        "${config.apiUrl}/${config.deployment}/completions?api-version=2024-12-01-preview",
        HttpMethod.POST,
        request,
      )

    return PromptService.Companion.PromptResult(
      response = response.body?.choices?.first()?.message?.content ?: throw RuntimeException(response.toString()),
      usage =
        response.body?.usage?.let {
          PromptResponseUsageDto(
            inputTokens = it.prompt_tokens,
            outputTokens = it.completion_tokens,
            cachedTokens = it.prompt_tokens_details?.cached_tokens,
          )
        },
    )
  }

  private fun HttpClientErrorException.parse(): JsonNode? {
    if (!this.responseBodyAsString.isNullOrBlank()) {
      return jacksonObjectMapper().readValue(this.responseBodyAsString)
    }
    return null
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    @Suppress("unused")
    class RequestBody(
      val max_completion_tokens: Long = 800,
      val stream: Boolean = false,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val response_format: RequestResponseFormat? = null,
      val stop: Boolean? = null,
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
      val image_url: RequestImageUrl,
    )

    class RequestImageUrl(
      val url: String,
    )

    class RequestResponseFormat(
      val type: String = "json_schema",
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val json_schema: Map<String, Any>? =
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
      val choices: List<ResponseChoice>,
      val usage: ResponseUsage,
    )

    class ResponseChoice(
      val message: ResponseMessage,
    )

    class ResponseMessage(
      val content: String,
    )

    class ResponseUsage(
      val prompt_tokens: Long,
      val completion_tokens: Long,
      val total_tokens: Long,
      val prompt_tokens_details: ResponsePromptTokenDetails?,
      val completion_tokens_details: ResponseCompletionTokenDetails?,
    )

    class ResponsePromptTokenDetails(
      val cached_tokens: Long,
    )

    class ResponseCompletionTokenDetails(
      val reasoning_tokens: Long,
      val accepted_prediction_tokens: Long,
      val rejected_prediction_tokens: Long,
    )
  }
}
