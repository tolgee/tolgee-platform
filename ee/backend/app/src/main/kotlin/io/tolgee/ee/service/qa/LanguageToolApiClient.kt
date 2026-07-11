package io.tolgee.ee.service.qa

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.util.logger
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.time.Duration
import java.util.concurrent.Semaphore

@Component
class LanguageToolApiClient(
  private val tolgeeProperties: TolgeeProperties,
  private val restTemplateBuilder: RestTemplateBuilder,
) {
  private val restTemplate: RestTemplate by lazy {
    restTemplateBuilder
      .connectTimeout(Duration.ofSeconds(tolgeeProperties.languageTool.connectTimeoutSeconds))
      .readTimeout(Duration.ofSeconds(tolgeeProperties.languageTool.readTimeoutSeconds))
      .build()
  }

  /**
   * Per-instance concurrency gate for `/v2/check` calls.
   */
  private val requestSemaphore: Semaphore by lazy {
    Semaphore(tolgeeProperties.languageTool.maxConcurrentRequests.coerceAtLeast(1), true)
  }

  val isConfigured: Boolean
    get() = tolgeeProperties.languageTool.url.isNotBlank()

  fun checkConfigured() {
    if (!isConfigured) {
      throw LanguageToolNotConfiguredException()
    }
  }

  @Cacheable(
    Caches.LANGUAGE_TOOL_RESULTS,
    key = "{#resolvedTag, T(org.apache.commons.codec.digest.DigestUtils).sha256Hex(#text)}",
  )
  fun callCheck(
    resolvedTag: String,
    text: String,
  ): List<LanguageToolMatch> {
    checkConfigured()
    return callCheckWithRetry(tolgeeProperties.languageTool.url, text, resolvedTag)
  }

  fun getLanguages(): List<LanguageToolLanguageInfo>? {
    checkConfigured()
    val response = getLanguagesApi(tolgeeProperties.languageTool.url) ?: return null
    return response.toList()
  }

  private fun callCheckWithRetry(
    baseUrl: String,
    text: String,
    languageTag: String,
  ): List<LanguageToolMatch> {
    repeat(LANGUAGE_TOOL_RETRY_ATTEMPTS - 1) { i ->
      try {
        return callCheckApi(baseUrl, text, languageTag)
      } catch (e: HttpClientErrorException) {
        // 4xx — unrecoverable; do not retry.
        throw e
      } catch (e: RestClientException) {
        logger.warn(
          "LanguageTool /v2/check failed (attempt {}/{}): {}",
          i + 1,
          LANGUAGE_TOOL_RETRY_ATTEMPTS,
          e.message,
        )
        Thread.sleep(LANGUAGE_TOOL_RETRY_BACKOFF_MS)
      }
    }
    return callCheckApi(baseUrl, text, languageTag)
  }

  private fun callCheckApi(
    baseUrl: String,
    text: String,
    languageTag: String,
  ): List<LanguageToolMatch> {
    val headers = HttpHeaders()
    headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

    val body =
      LinkedMultiValueMap<String, String>().apply {
        add("text", text)
        add("language", languageTag)
      }

    val request = HttpEntity(body, headers)
    val response =
      requestSemaphore.use {
        restTemplate.postForObject<LanguageToolResponse?>(
          "$baseUrl/v2/check",
          request,
        )
      }

    return response?.matches ?: emptyList()
  }

  private fun getLanguagesApi(baseUrl: String): Array<LanguageToolLanguageInfo>? {
    return restTemplate.getForObject<Array<LanguageToolLanguageInfo>?>(
      "$baseUrl/v2/languages",
    )
  }

  fun <T : Semaphore, R> T.use(block: (T) -> R): R {
    acquire()
    try {
      return block(this)
    } finally {
      release()
    }
  }

  companion object {
    private val logger = logger()
    const val LANGUAGE_TOOL_RETRY_ATTEMPTS = 3
    const val LANGUAGE_TOOL_RETRY_BACKOFF_MS = 100L
  }
}
