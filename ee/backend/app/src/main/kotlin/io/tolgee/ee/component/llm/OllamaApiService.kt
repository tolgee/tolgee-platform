package io.tolgee.ee.component.llm

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.NotEnoughTokensException
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.component.machineTranslation.providers.llm.LLMParams
import io.tolgee.configuration.tolgee.machineTranslation.LLMProviderInterface
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
import java.time.Duration
import java.util.*
import kotlin.time.measureTimedValue

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class OllamaApiService(
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

    val messages = mutableListOf<RequestMessage>()

    var promptHasJsonInside = false

    params.messages.forEach {
      if (
        it.type == LLMParams.Companion.LlmMessageType.TEXT &&
        it.text != null
      ) {
        messages.add(RequestMessage(role = "user", content = it.text))
        promptHasJsonInside = promptHasJsonInside || it.text.lowercase().contains("json")
      } else if (
        it.type == LLMParams.Companion.LlmMessageType.IMAGE &&
        it.image != null
      ) {
        messages.add(
          RequestMessage(
            role = "user",
            images = listOf("${Base64.getEncoder().encode(it.image)}"),
          ),
        )
      }
    }

    val requestBody =
      RequestBody(
        model = config.model!!,
        messages = messages,
        keepAlive = config.keepAlive,
        format = if (promptHasJsonInside && config.format == "json") "json" else null,
      )

    val request = HttpEntity(requestBody, headers)

    checkPositiveRateLimitTokens(params)

    val response: ResponseEntity<ResponseBody> =
      try {
        val (value, time) =
          measureTimedValue {
            restTemplate.exchange<ResponseBody>(
              "${config.apiUrl}/api/chat",
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
      response.body?.message?.content ?: throw RuntimeException(response.toString()),
      price = 0,
      usage = null,
    )
  }

  private fun checkPositiveRateLimitTokens(params: LLMParams) {
//    if (!params.isBatch) {
//      return
//    }

    try {
      tokenBucketManager.checkPositiveBalance(BUCKET_KEY)
    } catch (e: NotEnoughTokensException) {
      logger.debug(
        "Cannot translate using the translator for next " +
          "${Duration.ofMillis(e.refillAt - currentDateProvider.date.time).seconds}s. The bucket is empty.",
      )
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

    class RequestBody(
      val model: String,
      val messages: List<RequestMessage>,
      val stream: Boolean = false,
      val keepAlive: String? = null,
      val format: String? = "json",
    )

    class RequestMessage(
      val role: String,
      val content: String? = null,
      val images: List<String>? = null,
    )

    class ResponseBody(
      val model: String,
      val message: ResponseMessage,
    )

    class ResponseMessage(
      val role: String,
      val content: String,
    )
  }

  class TooManyRequestsData(
    val error: String? = null,
    val retryAfter: Int? = null,
  )
}
