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

  companion object {
    /**
     * Shared FROM-clause body for the batched TM lookup `inputs` CTE/subquery.
     *
     * Zips the two positionally-aligned bigint arrays via `unnest(:reqKeyIds, :reqLangIds)`
     * so the planner processes exactly `items.size` input rows (not the cross-product of
     * distinct keys × distinct langs), then resolves each pair's request key, project, and
     * base translation.
     *
     * Required bind parameters:
     *   - `:reqKeyIds`  `bigint[]` — request key ids, one per chunk item
     *   - `:reqLangIds` `bigint[]` — target language ids, positionally aligned with :reqKeyIds
     *
     * Callers wrap this with their own SELECT list; the EE batched path additionally joins
     * `language target_lang on target_lang.id = req.target_lang_id` to surface the language
     * tag needed by the stored-entries half.
     */
    const val BATCHED_INPUTS_FROM_BODY = """
      from unnest(cast(:reqKeyIds as bigint[]), cast(:reqLangIds as bigint[])) as req(req_key_id, target_lang_id)
      join key k_req on k_req.id = req.req_key_id
      join project proj_req on k_req.project_id = proj_req.id
      join translation req_base on req_base.key_id = k_req.id
        and req_base.language_id = proj_req.base_language_id
    """
  }
}
