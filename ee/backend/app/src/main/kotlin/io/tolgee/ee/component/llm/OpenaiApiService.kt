package io.tolgee.ee.component.llm

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
import io.tolgee.constants.Message
import io.tolgee.dtos.LLMParams
import io.tolgee.dtos.response.prompt.PromptResponseUsageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.util.Logging
import io.tolgee.util.logger
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
import kotlin.time.measureTimedValue

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiApiService(
  private val restTemplate: RestTemplate,
  private val tokenBucketManager: TokenBucketManager,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  fun translate(
    params: LLMParams,
    config: LLMProviderInterface,
  ): MtValueProvider.MtResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("api-key", config.apiKey)

    val messages = mutableListOf<OpenaiMessage>()

    var promptHasJsonInside = false

    params.messages.forEach {
      if (
        it.type == LLMParams.Companion.LlmMessageType.TEXT &&
        it.text != null
      ) {
        messages.add(OpenaiMessage(role = "user", content = it.text!!))
        promptHasJsonInside = promptHasJsonInside || it.text!!.lowercase().contains("json")
      } else if (
        it.type == LLMParams.Companion.LlmMessageType.IMAGE &&
        it.image != null
      ) {
        messages.add(
          OpenaiMessage(
            role = "user",
            content =
              listOf(
                OpenaiMessageContent(
                  type = "image_url",
                  image_url =
                    OpenaiImageUrl(
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
      OpenaiRequestBody(
        messages = messages,
        response_format =
          if (promptHasJsonInside) {
            when (config.format) {
              "json_object" -> OpenaiResponseFormat(type = "json_object", json_schema = null)
              "json_schema" -> OpenaiResponseFormat()
              else -> null
            }
          } else {
            null
          },
        model = config.model,
      )

    val request = HttpEntity(requestBody, headers)

    val response: ResponseEntity<OpenaiResponse> =
      try {
        val (value, time) =
          measureTimedValue {
            restTemplate.exchange<OpenaiResponse>(
              "${config.apiUrl}/${config.deployment}/completions?api-version=2024-12-01-preview",
              HttpMethod.POST,
              request,
            )
          }
        logger.debug("Translator request took ${time.inWholeMilliseconds} ms")
        value
      } catch (e: HttpClientErrorException.BadRequest) {
        e.parse()?.get("error")?.get("message")?.toString()?.let {
          throw BadRequestException(Message.LLM_PROVIDER_ERROR, listOf(it))
        }
        throw e
      } catch (e: HttpClientErrorException.TooManyRequests) {
        throw TranslationApiRateLimitException(currentDateProvider.date.time + 60 * 1000, cause = e)
      }

    return MtValueProvider.MtResult(
      response.body?.choices?.first()?.message?.content ?: throw RuntimeException(response.toString()),
      price = response.body?.usage?.total_tokens ?: 0,
      usage =
        response.body?.usage?.total_tokens?.let {
          PromptResponseUsageDto(
            totalTokens = it.toLong(),
            cachedTokens = response.body?.usage?.prompt_tokens_details?.cached_tokens?.toLong(),
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
    class OpenaiRequestBody(
      val max_completion_tokens: Long = 800,
      val stream: Boolean = false,
      @JsonInclude(JsonInclude.Include.NON_NULL)
      val response_format: OpenaiResponseFormat? = null,
      val stop: Boolean? = null,
      val messages: List<OpenaiMessage>,
      val model: String?,
      val temperature: Long? = 0,
    )

    class OpenaiMessage(
      val role: String,
      val content: Any,
    )

    class OpenaiMessageContent(
      val type: String,
      val image_url: OpenaiImageUrl,
    )

    class OpenaiImageUrl(
      val url: String,
    )

    class OpenaiResponseFormat(
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

    class OpenaiResponse(
      val choices: List<OpenAiResponseChoice>,
      val usage: OpenaiUsage,
    )

    class OpenAiResponseChoice(
      val message: OpenAiResponseMessage,
    )

    class OpenAiResponseMessage(
      val content: String,
    )

    class OpenaiUsage(
      val prompt_tokens: Int,
      val completion_tokens: Int,
      val total_tokens: Int,
      val prompt_tokens_details: OpenaiPromptTokenDetails?,
      val completion_tokens_details: OpenaiCompletionTokenDetails?,
    )

    class OpenaiPromptTokenDetails(
      val cached_tokens: Int,
    )

    class OpenaiCompletionTokenDetails(
      val reasoning_tokens: Int,
      val accepted_prediction_tokens: Int,
      val rejected_prediction_tokens: Int,
    )
  }
}
