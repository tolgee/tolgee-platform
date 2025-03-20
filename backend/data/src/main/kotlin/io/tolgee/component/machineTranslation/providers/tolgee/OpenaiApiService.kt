package io.tolgee.component.machineTranslation.providers.tolgee

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.nimbusds.jose.util.Base64
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.NotEnoughTokensException
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
import io.tolgee.util.Logging
import io.tolgee.util.debug
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
import java.time.Duration
import kotlin.time.measureTimedValue

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OpenaiApiService(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val restTemplate: RestTemplate,
  private val tokenBucketManager: TokenBucketManager,
  private val currentDateProvider: CurrentDateProvider,
) : Logging {
  fun translate(params: LLMParams): MtValueProvider.MtResult {
    val headers = HttpHeaders()
    headers.set("content-type", "application/json")
    headers.set("api-key", tolgeeMachineTranslationProperties.apiKey)

    val messages = mutableListOf<OpenaiMessage>()
    val content = mutableListOf<OpenaiMessageContent>()

    var promptHasJsonInside = false

    params.messages.forEach {
      if (
        it.type == LLMParams.Companion.LlmMessageType.TEXT
        && it.text != null
      ) {
        content.add(OpenaiMessageContent(type = "text", text = it.text))
        promptHasJsonInside = promptHasJsonInside || it.text.lowercase().contains("json")
      } else if (
        it.type == LLMParams.Companion.LlmMessageType.IMAGE
        && it.image != null
      ) {
        content.add(
          OpenaiMessageContent(
            type = "image_url",
            image_url = OpenaiImageUrl("data:image/jpeg;base64,${Base64.encode(it.image)}"),
          )
        )
      }
    }

    messages.add(
      OpenaiMessage(
        role = "user", content = content
      )
    )

    val requestBody = OpenaiRequestBody(
      messages = messages,
      response_format = if (promptHasJsonInside) OpenaiResponseFormat() else null
    )

    val request = HttpEntity(requestBody, headers)

    checkPositiveRateLimitTokens(params)

    val response: ResponseEntity<OpenaiResponse> = try {
      val (value, time) = measureTimedValue {
        restTemplate.exchange<OpenaiResponse>(
          "${tolgeeMachineTranslationProperties.apiUrl}/openai/deployments/4o/chat/completions?api-version=2023-03-15-preview",
          HttpMethod.POST,
          request,
        )
      }
      logger.debug("Translator request took ${time.inWholeMilliseconds} ms")
      value
    } catch (e: HttpClientErrorException.TooManyRequests) {
      val data = e.parse()
      emptyBucket(data)
      val waitTime = data.retryAfter ?: 0
      logger.debug("Translator thrown TooManyRequests exception. Waiting for ${waitTime}s")
      throw TranslationApiRateLimitException(currentDateProvider.date.time + (waitTime * 1000), e)
    }

    return MtValueProvider.MtResult(
      response.body?.choices?.first()?.message?.content ?: throw RuntimeException(response.toString()),
      100,
    )
  }

  private fun checkPositiveRateLimitTokens(params: LLMParams) {
//    if (!params.isBatch) {
//      return
//    }

    try {
      tokenBucketManager.checkPositiveBalance(BUCKET_KEY)
    } catch (e: NotEnoughTokensException) {
      logger.debug {
        "Cannot translate using the translator for next " + "${Duration.ofMillis(e.refillAt - currentDateProvider.date.time).seconds}s. The bucket is empty."
      }
      throw TranslationApiRateLimitException(e.refillAt, e)
    }
  }

  private fun emptyBucket(data: TooManyRequestsData) {
    val retryAfter = data.retryAfter ?: return
    tokenBucketManager.setEmptyUntil(BUCKET_KEY, currentDateProvider.date.time + retryAfter * 1000)
  }

  private fun HttpClientErrorException.TooManyRequests.parse(): TooManyRequestsData {
    return jacksonObjectMapper().readValue(this.responseBodyAsString)
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    const val BUCKET_KEY = "tolgee-translate-rate-limit"

    @Suppress("unused")
    class OpenaiRequestBody(
      val temperature: Long = 0,
      val max_tokens: Long = 800,
      val stream: Boolean = false,
      val response_format: OpenaiResponseFormat? = null,
      val stop: Boolean? = null,
      val messages: List<OpenaiMessage>
    )

    class OpenaiMessage(
      val role: String,
      val content: List<OpenaiMessageContent>,
    )

    class OpenaiMessageContent(
      val type: String,
      val text: String? = null,
      val image_url: OpenaiImageUrl? = null,
    )

    class OpenaiImageUrl(
      val url: String,
    )

    class OpenaiResponseFormat(
      val type: String = "json_object"
    )

    class OpenaiResponse(
      val choices: List<OpenAiResponseChoice>
    )

    class OpenAiResponseChoice(
      val message: OpenAiResponseMessage
    )

    class OpenAiResponseMessage(
      val content: String
    )
  }

  class TooManyRequestsData(
    val error: String? = null,
    val retryAfter: Int? = null,
  )
}
