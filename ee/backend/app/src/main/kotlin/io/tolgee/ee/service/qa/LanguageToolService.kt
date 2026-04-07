package io.tolgee.ee.service.qa

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.sentry.Sentry
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.logger
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject
import org.springframework.web.client.postForObject
import java.time.Duration

@Service
class LanguageToolService(
  private val tolgeeProperties: TolgeeProperties,
  private val restTemplateBuilder: RestTemplateBuilder,
) {
  private val restTemplate: RestTemplate =
    restTemplateBuilder
      .connectTimeout(Duration.ofSeconds(5))
      .readTimeout(Duration.ofSeconds(30))
      .build()

  private val resultCache: Cache<LanguageToolCacheKey, List<LanguageToolMatch>> =
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .maximumSize(200)
      .build()

  /**
   * Cache of supported languages fetched from the LanguageTool server.
   * Maps base language code (e.g., "en") to its default long code (e.g., "en-US").
   * Also contains entries for long codes mapping to themselves (e.g., "en-US" -> "en-US").
   */
  @Volatile
  private var supportedLanguages: Map<String, String>? = null

  fun check(
    text: String,
    languageTag: String,
  ): List<LanguageToolMatch> {
    if (text.isBlank()) return emptyList()

    val url = tolgeeProperties.languageTool.url
    if (url.isBlank()) {
      throw LanguageToolNotConfiguredException()
    }

    val resolvedTag = resolveLanguageTag(languageTag) ?: return emptyList()

    val key = LanguageToolCacheKey(resolvedTag, text)
    return resultCache.get(key) {
      callApi(url, text, resolvedTag)
    }
  }

  /**
   * Resolves a Tolgee language tag to a LanguageTool-compatible language code.
   *
   * Handles:
   * - Underscore to hyphen conversion (pt_BR -> pt-BR)
   * - Exact match against supported languages
   * - Fallback from unknown variants to base code's default variant (en-XX -> en-US)
   * - Returns null for unsupported languages
   */
  fun resolveLanguageTag(tag: String): String? {
    val normalized = tag.replace('_', '-')

    val languages = getSupportedLanguages()

    // Try exact match (e.g., "en-US", "de-DE", "pt-BR")
    if (languages.containsKey(normalized)) {
      return languages[normalized]
    }

    // Try base code (e.g., "en-XX" -> try "en")
    val base = normalized.split("-")[0]
    if (base == normalized) return null

    // Converts the base code to its default variant (e.g., "en" -> "en-US")
    return languages[base]
  }

  private fun getSupportedLanguages(): Map<String, String> {
    supportedLanguages?.let { return it }

    synchronized(this) {
      supportedLanguages?.let { return it }

      val url = tolgeeProperties.languageTool.url
      if (url.isBlank()) return emptyMap()

      val languages = fetchSupportedLanguages(url)
      if (languages.isNotEmpty()) {
        supportedLanguages = languages
      }
      return languages
    }
  }

  private fun fetchSupportedLanguages(baseUrl: String): Map<String, String> {
    return try {
      val response =
        restTemplate.getForObject<Array<LanguageToolLanguageInfo>?>(
          "$baseUrl/v2/languages",
        ) ?: return emptyMap()

      buildLanguageMap(response.toList())
    } catch (e: RestClientException) {
      Sentry.captureException(e)
      logger.warn("Failed to fetch supported languages from LanguageTool: ${e.message}")
      emptyMap()
    }
  }

  /**
   * Builds a map for language resolution:
   * - Each longCode maps to itself (e.g., "en-US" -> "en-US")
   * - Each base code maps to the first matching longCode (e.g., "en" -> "en-US")
   *
   * The /v2/languages response returns entries like:
   * [{"name": "English (US)", "code": "en", "longCode": "en-US"}, ...]
   *
   * For languages with variants (en, de, pt), the base code maps to whichever
   * variant appears first in the server's response (typically the "default" variant).
   */
  private fun buildLanguageMap(languages: List<LanguageToolLanguageInfo>): Map<String, String> {
    val map = mutableMapOf<String, String>()

    for (lang in languages) {
      val longCode = lang.longCode

      // Map the long code to itself
      map[longCode] = longCode

      // Map base code to the first variant found (acts as default)
      val base = lang.code
      if (!map.containsKey(base)) {
        map[base] = longCode
      }
    }

    return map
  }

  private fun callApi(
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
      restTemplate.postForObject<LanguageToolResponse?>(
        "$baseUrl/v2/check",
        request,
      )

    return response?.matches ?: emptyList()
  }

  companion object {
    private val logger = logger()
  }
}
