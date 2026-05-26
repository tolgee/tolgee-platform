package io.tolgee.service.translationMemory

import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView

/**
 * Resolves a TM match for auto-translate / batch pre-translate. The CE implementation
 * delegates to [io.tolgee.service.translation.TranslationMemoryService], which applies plan-aware
 * filtering internally (free plan → project's own TM only; paid plan → project + shared TMs).
 */
interface TmAutoTranslateProvider {
  fun getAutoTranslatedValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView?

  /**
   * Batched variant for the per-chunk pre-translate / auto-translate path. Resolves TM
   * matches for a list of `(keyId, targetLanguageId)` pairs in as few DB round-trips as
   * the implementation can manage.
   *
   * Default implementation loops [getAutoTranslatedValue] per item — correct but no
   * speedup. Implementations are expected to override with a single batched SQL.
   *
   * @param items chunk items as `(keyId, targetLanguageId)` pairs
   * @param keysById preloaded `Key` entities indexed by id (chunk processors already have these)
   * @param languagesById preloaded `Language` entities indexed by id
   * @return map keyed by the input pair; absence means "no TM match" (same as `null` per-item)
   */
  fun getAutoTranslatedValuesForChunk(
    items: List<Pair<Long, Long>>,
    keysById: Map<Long, Key>,
    languagesById: Map<Long, Language>,
  ): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    if (items.isEmpty()) return emptyMap()
    val result = HashMap<Pair<Long, Long>, TranslationMemoryItemView>(items.size)
    items.forEach { (keyId, langId) ->
      val key = keysById[keyId] ?: return@forEach
      val lang = languagesById[langId] ?: return@forEach
      getAutoTranslatedValue(key, lang)?.let { result[keyId to langId] = it }
    }
    return result
  }
}
