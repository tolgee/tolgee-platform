package io.tolgee.component.machineTranslation.providers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.NotEnoughTokensException
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.component.machineTranslation.metadata.Metadata
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
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
import kotlin.math.ceil
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class TolgeeTranslateApiService(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val restTemplate: RestTemplate,
  private val tokenBucketManager: TokenBucketManager,
  private val currentDateProvider: CurrentDateProvider
) : Logging {

  @OptIn(ExperimentalTime::class)
  fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult {
    val headers = HttpHeaders()
    headers.add("Something", null)

    val closeItems =
      params.metadata?.closeItems?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }
    val examples = params.metadata?.examples?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }

    val requestBody = TolgeeTranslateRequest(
      params.text,
      params.keyName,
      params.sourceTag,
      params.targetTag,
      examples,
      closeItems,
      priority = if (params.isBatch) "low" else "high"
    )
    val request = HttpEntity(requestBody, headers)

    consumeRateLimitTokens(params)

    val response: ResponseEntity<TolgeeTranslateResponse> = try {
      val (value, time) = measureTimedValue {
        restTemplate.exchange<TolgeeTranslateResponse>(
          "${tolgeeMachineTranslationProperties.url}/api/openai/translate",
          HttpMethod.POST,
          request
        )
      }
      logger.debug("Translator request took ${time.inWholeMilliseconds} ms")
      value
    } catch (e: HttpClientErrorException.TooManyRequests) {
      val data = e.parse()
      tokenBucketManager.addTokens(TOKEN_BUCKET_KEY, tolgeeMachineTranslationProperties.tokensToPreConsume)
      syncBuckets(data)
      val waitTime = data.retryAfter ?: 0
      logger.debug("Translator thrown TooManyRequests exception. Waiting for ${waitTime}s")
      throw TranslationApiRateLimitException(currentDateProvider.date.time + (waitTime * 1000), e)
    }

    val costString = response.headers.get("Mt-Credits-Cost")?.singleOrNull()
      ?: throw IllegalStateException("No valid Credits-Cost header in response")
    val cost = costString.toInt()

    finalizeTokenCost(params.isBatch, cost)

    return MtValueProvider.MtResult(
      response.body?.output
        ?: throw RuntimeException(response.toString()),
      ceil(cost * tolgeeMachineTranslationProperties.tokensToMtCredits).toInt(),
      response.body?.contextDescription,
    )
  }

  private fun finalizeTokenCost(isBatch: Boolean, cost: Int) {
    if (!isBatch) {
      return
    }
    tokenBucketManager.addTokens(
      TOKEN_BUCKET_KEY,
      tolgeeMachineTranslationProperties.tokensToPreConsume - cost
    )
  }

  private fun consumeRateLimitTokens(params: TolgeeTranslateParams) {
    if (!params.isBatch) {
      return
    }

    try {
      tokenBucketManager.consume(
        TOKEN_BUCKET_KEY,
        tolgeeMachineTranslationProperties.tokensToPreConsume,
        getTokensRateLimitTokensPerInterval(),
        BUCKET_INTERVAL
      )
    } catch (e: NotEnoughTokensException) {
      logger.debug(
        "Not enough token rate limit tokens to translate. " +
          "Tokens will be refilled at ${Duration.ofMillis(e.refillAt - currentDateProvider.date.time).seconds}s"
      )
      throw TranslationApiRateLimitException(e.refillAt, e)
    }

    try {
      tokenBucketManager.consume(
        CALL_BUCKET_KEY,
        1,
        getCallRateLimitTokensPerInterval(),
        BUCKET_INTERVAL
      )
    } catch (e: NotEnoughTokensException) {
      logger.debug(
        "Not enough call rate limit tokens to translate. " +
          "Tokens will be refilled at ${Duration.ofMillis(e.refillAt - currentDateProvider.date.time).seconds}s"
      )
      throw TranslationApiRateLimitException(e.refillAt, e)
    }
  }

  private fun syncBuckets(data: TooManyRequestsData) {
    val retryAfter = data.retryAfter ?: return
    val bucketKey = when (data.rateLimit) {
      "token" -> TOKEN_BUCKET_KEY
      "call" -> CALL_BUCKET_KEY
      else -> return
    }

    tokenBucketManager.setEmptyUntil(bucketKey, currentDateProvider.date.time + retryAfter * 1000)
  }

  private fun getTokensRateLimitTokensPerInterval(): Long {
    return tolgeeMachineTranslationProperties.batchMaxTokensPerMinute * BUCKET_INTERVAL.seconds / 60
  }

  private fun getCallRateLimitTokensPerInterval(): Long {
    return tolgeeMachineTranslationProperties.batchMaxCallsPerMinute * BUCKET_INTERVAL.seconds / 60
  }

  /**
   * Data structure for mapping the AzureCognitive JSON response objects.
   */
  companion object {
    private const val TOKEN_BUCKET_KEY = "tolgee-translate-token-rate-limit"
    private const val CALL_BUCKET_KEY = "tolgee-translate-call-rate-limit"

    private val BUCKET_INTERVAL = Duration.ofMinutes(1)

    class TolgeeTranslateRequest(
      val input: String,
      val keyName: String?,
      val source: String,
      val target: String?,
      val examples: List<TolgeeTranslateExample>?,
      val closeItems: List<TolgeeTranslateExample>?,
      val priority: String = "low"
    )

    class TolgeeTranslateParams(
      val text: String,
      val keyName: String?,
      val sourceTag: String,
      val targetTag: String,
      val metadata: Metadata?,
      val isBatch: Boolean
    )

    class TolgeeTranslateExample(
      var keyName: String,
      var source: String,
      var target: String
    )

    class TolgeeTranslateResponse(val output: String, val contextDescription: String?)
  }

  class TooManyRequestsData(
    val error: String? = null,
    val retryAfter: Int? = null,
    val rateLimit: String? = null
  )
}

private fun HttpClientErrorException.TooManyRequests.parse(): TolgeeTranslateApiService.TooManyRequestsData {
  return jacksonObjectMapper().readValue(this.responseBodyAsString)
}
