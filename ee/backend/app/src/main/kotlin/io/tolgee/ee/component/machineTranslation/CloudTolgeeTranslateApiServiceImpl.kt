package io.tolgee.ee.component.machineTranslation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.bucket.NotEnoughTokensException
import io.tolgee.component.bucket.TokenBucketManager
import io.tolgee.component.machineTranslation.MtValueProvider
import io.tolgee.component.machineTranslation.TranslationApiRateLimitException
import io.tolgee.component.machineTranslation.providers.tolgee.CloudTolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.CloudTolgeeTranslateApiService.Companion.BUCKET_KEY
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateApiService
import io.tolgee.component.machineTranslation.providers.tolgee.TolgeeTranslateParams
import io.tolgee.configuration.tolgee.machineTranslation.TolgeeMachineTranslationProperties
import io.tolgee.model.mtServiceConfig.Formality
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
class CloudTolgeeTranslateApiServiceImpl(
  private val tolgeeMachineTranslationProperties: TolgeeMachineTranslationProperties,
  private val restTemplate: RestTemplate,
  private val tokenBucketManager: TokenBucketManager,
  private val currentDateProvider: CurrentDateProvider,
) : Logging, TolgeeTranslateApiService, CloudTolgeeTranslateApiService {
  override fun translate(params: TolgeeTranslateParams): MtValueProvider.MtResult {
    val headers = HttpHeaders()

    val closeItems =
      params.metadata?.closeItems?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }
    val examples = params.metadata?.examples?.map { item -> TolgeeTranslateExample(item.key, item.source, item.target) }
    val glossary =
      params.metadata?.glossaryTerms?.map { item ->
        TolgeeTranslateGlossaryItem(
          item.source,
          item.target,
          item.description,
          item.isNonTranslatable,
          item.isCaseSensitive,
          item.isAbbreviation,
          item.isForbiddenTerm,
        )
      }

    val requestBody =
      TolgeeTranslateRequest(
        params.text,
        params.keyName,
        params.metadata?.keyDescription,
        params.sourceTag,
        params.targetTag,
        examples,
        glossary,
        closeItems,
        priority = if (params.isBatch) "low" else "high",
        params.formality,
        params.metadata?.projectDescription,
        params.metadata?.languageDescription,
        params.pluralForms,
        params.pluralFormExamples,
      )

    val request = HttpEntity(requestBody, headers)

    checkPositiveRateLimitTokens(params)

    val response: ResponseEntity<TolgeeTranslateResponse> =
      try {
        val (value, time) =
          measureTimedValue {
            restTemplate.exchange<TolgeeTranslateResponse>(
              "${tolgeeMachineTranslationProperties.url}/api/openai/translate",
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
      response.body?.output
        ?: throw RuntimeException(response.toString()),
      params.text.length * 100,
      response.body?.contextDescription,
    )
  }

  private fun checkPositiveRateLimitTokens(params: TolgeeTranslateParams) {
    if (!params.isBatch) {
      return
    }

    try {
      tokenBucketManager.checkPositiveBalance(BUCKET_KEY)
    } catch (e: NotEnoughTokensException) {
      logger.debug {
        "Cannot translate using the translator for next " +
          "${Duration.ofMillis(e.refillAt - currentDateProvider.date.time).seconds}s. The bucket is empty."
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
    @Suppress("unused")
    class TolgeeTranslateRequest(
      val input: String,
      val keyName: String?,
      val contextDescription: String?,
      val source: String,
      val target: String?,
      val examples: List<TolgeeTranslateExample>?,
      val glossary: List<TolgeeTranslateGlossaryItem>?,
      val closeItems: List<TolgeeTranslateExample>?,
      val priority: String = "low",
      val formality: Formality? = null,
      val projectDescription: String? = null,
      val languageNote: String? = null,
      val pluralForms: Map<String, String>? = null,
      val pluralFormExamples: Map<String, String>? = null,
    )

    class TolgeeTranslateExample(
      var keyName: String,
      var source: String,
      var target: String,
    )

    class TolgeeTranslateGlossaryItem(
      var source: String,
      var target: String? = null,
      var description: String? = null,
      var isNonTranslatable: Boolean? = null,
      var isCaseSensitive: Boolean? = null,
      var isAbbreviation: Boolean? = null,
      var isForbiddenTerm: Boolean? = null,
    )

    class TolgeeTranslateResponse(val output: String, val contextDescription: String?)
  }

  class TooManyRequestsData(
    val error: String? = null,
    val retryAfter: Int? = null,
  )
}
