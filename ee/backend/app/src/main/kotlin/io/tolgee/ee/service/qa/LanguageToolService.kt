package io.tolgee.ee.service.qa

import io.sentry.Sentry
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException

@Service
class LanguageToolService(
  private val languageToolApiClient: LanguageToolApiClient,
) {
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
    languageToolApiClient.checkConfigured()
    val resolvedTag = resolveLanguageTag(languageTag) ?: return emptyList()
    return languageToolApiClient.callCheck(resolvedTag, text)
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

      if (!languageToolApiClient.isConfigured) return emptyMap()

      val languages = fetchSupportedLanguages()
      if (languages.isNotEmpty()) {
        supportedLanguages = languages
      }
      return languages
    }
  }

  private fun fetchSupportedLanguages(): Map<String, String> {
    try {
      val supportedLanguages = languageToolApiClient.getLanguages() ?: return emptyMap()
      return buildLanguageMap(supportedLanguages)
    } catch (e: RestClientException) {
      Sentry.captureException(e)
      logger.warn("Failed to fetch supported languages from LanguageTool: ${e.message}")
      return emptyMap()
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

  companion object {
    private val logger = logger()
  }
}
