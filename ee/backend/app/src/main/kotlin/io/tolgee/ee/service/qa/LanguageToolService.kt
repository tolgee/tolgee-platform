package io.tolgee.ee.service.qa

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.languagetool.JLanguageTool
import org.languagetool.Language
import org.languagetool.Languages
import org.languagetool.rules.RuleMatch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class LanguageToolService {
  private val threadLocalInstances: ThreadLocal<MutableMap<String, JLanguageTool>> =
    ThreadLocal.withInitial { mutableMapOf() }

  private val resultCache: Cache<LanguageToolCacheKey, List<RuleMatch>> =
    Caffeine
      .newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .maximumSize(200)
      .build()

  fun check(
    text: String,
    languageTag: String,
  ): List<RuleMatch> {
    if (text.isBlank()) return emptyList()

    val language = resolveLanguage(languageTag) ?: return emptyList()

    val key = LanguageToolCacheKey(language.shortCodeWithCountryAndVariant, text)
    return resultCache.get(key) {
      val lt = getOrCreateInstance(language)
      lt.check(text)
    }
  }

  fun isLanguageSupported(languageTag: String): Boolean {
    return resolveLanguage(languageTag) != null
  }

  fun resolveLanguage(tag: String): Language? {
    val exact = languageForShortCodeOrNull(tag)
    if (exact != null) {
      return exact.withDefaultVariantIfBase()
    }

    // Try base language code for tags with separators (e.g., "en_US" → "en", "en-XX" → "en")
    // LanguageTool only recognizes hyphens as separators, so underscore tags and
    // unregistered regional variants fall back to the base code lookup.
    val base = tag.split("-", "_")[0]
    if (base == tag) return null
    val baseLanguage = languageForShortCodeOrNull(base) ?: return null
    return baseLanguage.withDefaultVariantIfBase()
  }

  /**
   * If this is a base language (e.g., "en" without a country), return its default variant
   * (e.g., "en-US") which has full spelling/grammar rules. If this is already a specific
   * variant (e.g., "en-GB", "pt-BR"), return it as-is to preserve region-specific rules.
   */
  private fun Language.withDefaultVariantIfBase(): Language {
    val isBase = shortCode == shortCodeWithCountryAndVariant
    return if (isBase) defaultLanguageVariant else this
  }

  private fun languageForShortCodeOrNull(code: String): Language? =
    try {
      Languages.getLanguageForShortCode(code)
    } catch (_: Exception) {
      null
    }

  private fun getOrCreateInstance(language: Language): JLanguageTool {
    val instances = threadLocalInstances.get()
    return instances.getOrPut(language.shortCodeWithCountryAndVariant) {
      logger.debug("Creating JLanguageTool instance for language: ${language.shortCodeWithCountryAndVariant}")
      JLanguageTool(language)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(LanguageToolService::class.java)
  }
}
