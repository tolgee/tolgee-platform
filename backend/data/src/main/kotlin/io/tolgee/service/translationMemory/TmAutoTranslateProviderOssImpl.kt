package io.tolgee.service.translationMemory

import io.tolgee.Metrics
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationService
import org.springframework.stereotype.Component

/**
 * Resolves a TM match for auto-translate / batch pre-translate. The OSS path performs an
 * exact-equality lookup against the project's own translations — no trigram similarity —
 * because this method runs once per key in the batch / MT pipelines and any per-call cost
 * scales linearly with project size. Similarity-based suggestions live in
 * [io.tolgee.service.translation.TranslationMemoryService.getSuggestions] and power the
 * editor UI only.
 *
 * The EE module overrides this bean with `@Primary` to also consult stored TM entries
 * (manual entries + TMX imports) on every readable TM before falling back here.
 */
@Component
class TmAutoTranslateProviderOssImpl(
  private val translationService: TranslationService,
  private val metrics: Metrics,
) : TmAutoTranslateProvider {
  override fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val startedAt = System.currentTimeMillis()
    return try {
      val result = translationService.getTranslationMemoryValue(key, targetLanguage)
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
