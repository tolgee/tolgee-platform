package io.tolgee.service.translationMemory

import io.tolgee.Metrics
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
  private val entityManager: EntityManager,
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

  /**
   * Batched exact-match TM lookup for a whole chunk. Mirrors the per-item HQL query in
   * [io.tolgee.repository.TranslationRepository.getTranslationMemoryValue] (exact equality
   * on base text, cross-project) but resolves all `(keyId, targetLanguageId)` pairs in
   * a single SQL round-trip via a LATERAL join with `LIMIT 1` per pair.
   *
   * Self-key exclusion (`k.id <> inputs.req_key_id`) is enforced inside the LATERAL body,
   * so we can use `LIMIT 1` instead of needing top-K + post-filtering.
   */
  @Transactional
  override fun getAutoTranslatedValuesForChunk(
    items: List<Pair<Long, Long>>,
    keysById: Map<Long, Key>,
    languagesById: Map<Long, Language>,
  ): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    if (items.isEmpty()) return emptyMap()
    val startedAt = System.currentTimeMillis()
    val result = fetchUnmetered(items)
    metrics.recordTranslationMemoryLookupBatch(
      chunkSize = items.size,
      hitCount = result.size,
      durationMs = System.currentTimeMillis() - startedAt,
    )
    return result
  }

  /**
   * Same SQL as [getAutoTranslatedValuesForChunk] but does not emit metrics. Intended for
   * the EE provider, which calls this as a fallback inside its own metered orchestration —
   * recording metrics here would double-count items and emit two timer samples per chunk.
   * External callers should use [getAutoTranslatedValuesForChunk] instead.
   */
  @Transactional
  fun fetchUnmetered(items: List<Pair<Long, Long>>): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    if (items.isEmpty()) return emptyMap()

    // Pass the actual (keyId, targetLangId) pairs as two positionally-aligned arrays.
    // Postgres `unnest(arr1, arr2)` zips them into rows, so the input set is exactly
    // O(items) instead of O(uniqueKeys × uniqueLangs) — important when a chunk straddles
    // the cross-product (e.g. partial coverage of (key × lang) combinations).
    val reqKeyIds = items.map { it.first }.toTypedArray()
    val reqLangIds = items.map { it.second }.toTypedArray()

    val rows =
      entityManager
        .createNativeQuery(
          """
          select inputs.req_key_id, inputs.target_lang_id, m.source_key_id, m.target_text, m.key_name, inputs.base_text
          from (
            select req.req_key_id as req_key_id, req.target_lang_id as target_lang_id, req_base.text as base_text
            ${TmAutoTranslateProvider.BATCHED_INPUTS_FROM_BODY}
          ) inputs
          cross join lateral (
            select k.id as source_key_id, target.text as target_text, k.name as key_name
            from translation source_base
            join key k on source_base.key_id = k.id and k.id <> inputs.req_key_id
            join project source_p on k.project_id = source_p.id
              and source_base.language_id = source_p.base_language_id
            join translation target on target.key_id = k.id
              and target.language_id = inputs.target_lang_id
              and target.text is not null
              and target.text <> ''
            where source_base.text = inputs.base_text
            limit 1
          ) m
          """,
        ).setParameter("reqKeyIds", reqKeyIds)
        .setParameter("reqLangIds", reqLangIds)
        .resultList

    val result = HashMap<Pair<Long, Long>, TranslationMemoryItemView>(rows.size)
    rows.forEach { row ->
      row as Array<*>
      val reqKeyId = (row[0] as Number).toLong()
      val targetLangId = (row[1] as Number).toLong()
      val pair = reqKeyId to targetLangId
      val sourceKeyId = (row[2] as Number).toLong()
      val targetText = row[3] as String
      val keyName = row[4] as String
      val baseText = row[5] as String
      result[pair] =
        TranslationMemoryItemView(
          targetTranslationText = targetText,
          baseTranslationText = baseText,
          keyName = keyName,
          keyNamespace = null,
          similarity = 1f,
          keyId = sourceKeyId,
        )
    }
    return result
  }
}
