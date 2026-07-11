package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import io.sentry.Sentry
import io.tolgee.configuration.tolgee.machineTranslation.LlmProviderInterface
import io.tolgee.dtos.LlmParams
import io.tolgee.dtos.PromptResult
import io.tolgee.ee.api.v2.hateoas.model.prompt.PromptResponseUsageModel
import io.tolgee.exceptions.LlmContentFilterException
import io.tolgee.exceptions.LlmEmptyResponseException
import io.tolgee.model.enums.LlmProviderType
import io.tolgee.util.Logging
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiApiService :
  AbstractLlmApiService(),
  Logging {
  override fun defaultAttempts(): List<Int> = listOf(60, 120)

  override fun translate(
    params: LlmParams,
    config: LlmProviderInterface,
    restTemplate: RestTemplate,
  ): PromptResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("Authorization", "Bearer ${config.apiKey}")
    headers.set("api-key", config.apiKey)

    val content = getContent(params)

    val messages = listOf(RequestMessage(role = "user", content = content))

    val requestBody =
      RequestBody(
        max_completion_tokens = config.maxTokens,
        messages = messages,
        response_format =
          if (params.shouldOutputJson) {
            when (config.format) {
              "json_schema" -> RequestResponseFormat()
              else -> null
            }
          } else {
            null
          },
        model = config.model,
        reasoning_effort = config.reasoningEffort,
      )

    val request = HttpEntity(requestBody, headers)

    val url =
      if (config.type === LlmProviderType.OPENAI) {
        "${config.apiUrl}/v1/chat/completions"
      } else {
        "${config.apiUrl}/openai/deployments/${config.deployment}/chat/completions?api-version=2025-01-01-preview"
      }

    val response: ResponseEntity<ResponseBody> =
      try {
        restTemplate.exchange(
          url,
          HttpMethod.POST,
          request,
          ResponseBody::class.java,
        )
      } catch (e: HttpClientErrorException) {
        if (e.statusCode == HttpStatus.BAD_REQUEST) {
          val body = parseErrorBody(e)
          if (body?.get("error")?.get("code")?.asText() == "content_filter") {
            throw LlmContentFilterException()
          }
        }
        throw e
      }

    setSentryContext(request, response)

    return PromptResult(
      response =
        response.body
          ?.choices
          ?.firstOrNull()
          ?.message
          ?.content
          ?: throw LlmEmptyResponseException(),
      usage =
        response.body?.usage?.let {
          PromptResult.Usage(
            inputTokens = it.prompt_tokens,
            outputTokens = it.completion_tokens,
            cachedTokens = it.prompt_tokens_details?.cached_tokens,
          )
        },
    )
  }

  fun getContent(params: LlmParams): MutableList<RequestMessageContent> {
    val content = mutableListOf<RequestMessageContent>()

    params.messages.forEach {
      if (
        it.type == LlmParams.Companion.LlmMessageType.TEXT &&
        it.text != null
      ) {
        content.add(RequestMessageContent("text", text = it.text!!))
      } else if (
        it.type == LlmParams.Companion.LlmMessageType.IMAGE &&
        it.image != null
      ) {
        content.add(
          RequestMessageContent(
            type = "image_url",
            image_url =
              RequestImageUrl(
                "data:image/jpeg;base64,${it.image}",
              ),
          ),
        )
      }
    }

    if (params.shouldOutputJson) {
      content.add(
        RequestMessageContent(type = "text", text = "Strictly return only valid json!"),
      )
    }

    return content
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
      val messages: List<RequestMessage>,
      val model: String?,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val reasoning_effort: String? = null,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val temperature: Long? = null,
    )

    class RequestMessage(
      val role: String,
      val content: Any,
    )

    class RequestMessageContent(
      val type: String,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val image_url: RequestImageUrl? = null,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val text: String? = null,
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
      val content: String?,
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
