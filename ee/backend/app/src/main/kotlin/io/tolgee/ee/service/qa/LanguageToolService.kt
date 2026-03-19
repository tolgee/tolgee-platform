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

    val key = LanguageToolCacheKey(language.shortCode, text)
    return resultCache.get(key) {
      val lt = getOrCreateInstance(language)
      lt.check(text)
    }
  }

  fun isLanguageSupported(languageTag: String): Boolean {
    return resolveLanguage(languageTag) != null
  }

  private fun resolveLanguage(tag: String): Language? {
    // Try exact match first (e.g., "en-US", "pt-BR")
    val exact =
      try {
        Languages.getLanguageForShortCode(tag)
      } catch (_: Exception) {
        null
      }
    if (exact != null) {
      // Prefer the default variant if the exact match is a base language
      // (e.g., base "en" has no spelling rules, but "en-US" does)
      return exact.defaultLanguageVariant ?: exact
    }

    // Try base language code (e.g., "en-US" → "en")
    val base = tag.split("-", "_")[0]
    if (base != tag) {
      val baseLanguage =
        try {
          Languages.getLanguageForShortCode(base)
        } catch (_: Exception) {
          null
        }
      if (baseLanguage != null) {
        return baseLanguage.defaultLanguageVariant ?: baseLanguage
      }
    }

    // Find the first available variant matching the base code
    return Languages.get().firstOrNull { lang ->
      lang.shortCode.split("-", "_")[0] == base
    }
  }

  private fun getOrCreateInstance(language: Language): JLanguageTool {
    val instances = threadLocalInstances.get()
    return instances.getOrPut(language.shortCode) {
      logger.debug("Creating JLanguageTool instance for language: ${language.shortCode}")
      JLanguageTool(language)
    }
  }

  companion object {
    private val logger = LoggerFactory.getLogger(LanguageToolService::class.java)
  }
}
