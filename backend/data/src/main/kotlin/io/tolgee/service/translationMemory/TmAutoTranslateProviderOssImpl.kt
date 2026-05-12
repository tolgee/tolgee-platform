package io.tolgee.service.translationMemory

import io.tolgee.Metrics
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationMemoryService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

/**
 * Resolves a TM match for auto-translate / batch pre-translate. Plan filtering is applied
 * inside [TranslationMemoryService] — free plan only sees the project's own TM, paid plan
 * sees project + shared TMs.
 */
@Component
class TmAutoTranslateProviderOssImpl(
  private val translationMemoryService: TranslationMemoryService,
  private val metrics: Metrics,
) : TmAutoTranslateProvider {
  override fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val startedAt = System.currentTimeMillis()
    return try {
      val result =
        translationMemoryService
          .getSuggestions(key, targetLanguage.tag, PageRequest.of(0, 1))
          .content
          .firstOrNull()
      metrics.recordTranslationMemoryLookup(
        outcome = if (result != null) "hit" else "miss",
        durationMs = System.currentTimeMillis() - startedAt,
      )
      result
    } catch (e: Exception) {
      metrics.recordTranslationMemoryLookup(
        outcome = "error",
        durationMs = System.currentTimeMillis() - startedAt,
      )
      throw e
    }
  }
}
