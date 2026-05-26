package io.tolgee.ee.service.translationMemory

import io.tolgee.Metrics
import io.tolgee.model.Language
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translation.TranslationService
import io.tolgee.service.translationMemory.TmAutoTranslateProvider
import io.tolgee.service.translationMemory.TmAutoTranslateProviderOssImpl
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * EE auto-translate resolver. Mirrors the suggestion panel's notion of "matches the project
 * can see" but with exact equality instead of trigram similarity, so the per-key latency
 * stays in the millisecond range required by the batch / MT pipelines.
 *
 * Two sources of matches, in priority order:
 *   1. Stored entries on any TM the project has read access to (`translation_memory_entry`
 *      rows imported via TMX or added through the entry dialog).
 *   2. Virtual rows: translations on any project that has write access to a readable TM,
 *      where that project's base translation equals our base text. Mirrors the virtual-row
 *      half of [io.tolgee.ee.service.translationMemory.TranslationMemoryServiceEeImpl.baseSelect].
 *
 * Plan-aware filtering is handled by [TranslationMemoryManagementService.getReadableTmIdsForSuggestions]:
 * when the feature is disabled it returns only the project's own TM, so this override
 * degrades gracefully on free-tier orgs without behaving differently from the OSS path
 * (project-type TMs rarely carry stored entries).
 *
 * Penalty filter: only TMs whose effective penalty (per-assignment override, falling back to
 * the TM's `default_penalty`) is 0 contribute matches. Any non-zero penalty means the user
 * has marked the TM as "not fully trusted" — the suggestion panel still surfaces it with the
 * penalty subtracted from similarity, but auto-translate must stay out so a 90%-trusted hit
 * doesn't silently overwrite a fresh empty slot.
 *
 * Self-match protection: virtual rows from the current key are excluded so a project
 * doesn't auto-fill from itself.
 */
@Component
@Primary
class TmAutoTranslateProviderEeImpl(
  private val ossDelegate: TmAutoTranslateProviderOssImpl,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
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
      val result =
        findExactMatch(key, targetLanguage)
          ?: ossDelegate.getAutoTranslatedValue(key, targetLanguage)
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
   * Batched EE auto-translate lookup. Issues at most **two** SQL round-trips per chunk:
   *  1. The EE-aware UNION ALL (stored entries + virtual rows, scoped to readable `tmIds`,
   *     enforcing `write_access` and `penalty = 0`), LATERAL-joined against an `inputs` CTE
   *     so all `(reqKeyId, targetLanguageId)` pairs are resolved in a single statement.
   *  2. For any pair that didn't get an EE hit, [TmAutoTranslateProviderOssImpl] is called
   *     once with the remaining pairs — mirroring the per-item path's "EE-then-OSS-fallback"
   *     layering.
   *
   * Compared to the per-item path's ~2 round-trips per chunk item, this is bounded at 2
   * round-trips per chunk regardless of chunk size.
   */
  @Transactional
  override fun getAutoTranslatedValuesForChunk(
    items: List<Pair<Long, Long>>,
    keysById: Map<Long, Key>,
    languagesById: Map<Long, Language>,
  ): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    if (items.isEmpty()) return emptyMap()
    val startedAt = System.currentTimeMillis()

    // All chunk items share a project (a batch job scopes to one). Pick any preloaded key
    // to resolve projectId / organizationId — if none of the items have a Key in the map
    // we can't determine the project, so just defer everything to OSS (defensive — chunk
    // processors filter to known keys before calling).
    val anyKey =
      items.firstNotNullOfOrNull { keysById[it.first] }
        ?: return runFullOssFallback(items, startedAt)

    val projectId = anyKey.project.id
    val organizationId = anyKey.project.organizationOwner.id
    val tmIds =
      translationMemoryManagementService.getReadableTmIdsForSuggestions(
        projectId = projectId,
        organizationId = organizationId,
      )

    // No readable TMs (free plan with the feature off, etc.) — same effect as a UNION ALL
    // returning no rows. Just hit OSS for all pairs.
    if (tmIds.isEmpty()) return runFullOssFallback(items, startedAt)

    val eeMatches = findExactMatchesBatched(items, tmIds, projectId, keysById)
    val unmatched = items.filterNot { it in eeMatches }

    val ossMatches =
      if (unmatched.isEmpty()) {
        emptyMap()
      } else {
        // fetchUnmetered: this EE call emits its own batch metric after merging; the OSS
        // public method would record again, double-counting items in the same chunk.
        ossDelegate.fetchUnmetered(unmatched)
      }

    val combined = HashMap<Pair<Long, Long>, TranslationMemoryItemView>(eeMatches.size + ossMatches.size)
    combined.putAll(ossMatches)
    combined.putAll(eeMatches)
    metrics.recordTranslationMemoryLookupBatch(
      chunkSize = items.size,
      hitCount = combined.size,
      durationMs = System.currentTimeMillis() - startedAt,
    )
    return combined
  }

  private fun runFullOssFallback(
    items: List<Pair<Long, Long>>,
    startedAt: Long,
  ): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    // fetchUnmetered: EE records the chunk's batch metric below, so the OSS public method's
    // record would emit a duplicate timer sample and double-count items.
    val result = ossDelegate.fetchUnmetered(items)
    metrics.recordTranslationMemoryLookupBatch(
      chunkSize = items.size,
      hitCount = result.size,
      durationMs = System.currentTimeMillis() - startedAt,
    )
    return result
  }

  /**
   * Returns EE-aware matches for the given chunk in one SQL round-trip. Pairs without
   * a stored- or virtual-row match are absent from the result map (caller is expected
   * to fall back to OSS for them, like the per-item path does).
   *
   * The query mirrors the per-item `findExactMatch` UNION ALL exactly — same `tmIds`
   * filter, same penalty gate, same `write_access` requirement on virtual contributors,
   * same self-key exclusion — wrapped in a LATERAL JOIN with `LIMIT 1` per input pair
   * so the cost stays bounded.
   */
  private fun findExactMatchesBatched(
    items: List<Pair<Long, Long>>,
    tmIds: List<Long>,
    projectId: Long,
    keysById: Map<Long, Key>,
  ): Map<Pair<Long, Long>, TranslationMemoryItemView> {
    // Pass the actual (keyId, targetLangId) pairs as two positionally-aligned arrays.
    // Postgres `unnest(arr1, arr2)` zips them into rows, so the input set is exactly
    // O(items) instead of O(uniqueKeys × uniqueLangs).
    val reqKeyIds = items.map { it.first }.toTypedArray()
    val reqLangIds = items.map { it.second }.toTypedArray()

    val sql =
      """
      with inputs as (
        select req.req_key_id as req_key_id,
               req.target_lang_id as target_lang_id,
               target_lang.tag as target_lang_tag,
               req_base.text as base_text
        from unnest(cast(:reqKeyIds as bigint[]), cast(:reqLangIds as bigint[])) as req(req_key_id, target_lang_id)
        join key k_req on k_req.id = req.req_key_id
        join project proj_req on k_req.project_id = proj_req.id
        join translation req_base on req_base.key_id = k_req.id
          and req_base.language_id = proj_req.base_language_id
        join language target_lang on target_lang.id = req.target_lang_id
      )
      select inputs.req_key_id, inputs.target_lang_id, m.target_text, inputs.base_text
      from inputs
      cross join lateral (
        select target_text, kind from (
          select e.target_text, 'stored' as kind
          from translation_memory_entry e
          join translation_memory tm on tm.id = e.translation_memory_id
          left join translation_memory_project tmp_recv
            on tmp_recv.translation_memory_id = e.translation_memory_id
           and tmp_recv.project_id = :projectId
          where e.translation_memory_id in (:tmIds)
            and e.source_text = inputs.base_text
            and e.target_language_tag = inputs.target_lang_tag
            and e.target_text <> ''
            and coalesce(tmp_recv.penalty, tm.default_penalty) = 0

          union all

          select target_t.text as target_text, 'virtual' as kind
          from translation base_t
          join key k on k.id = base_t.key_id and k.deleted_at is null and k.id <> inputs.req_key_id
          join project p
            on p.id = k.project_id
           and p.base_language_id = base_t.language_id
           and p.deleted_at is null
          join translation_memory_project tmp_w
            on tmp_w.project_id = p.id and tmp_w.write_access = true
          join translation_memory tm_virt
            on tm_virt.id = tmp_w.translation_memory_id
           and tm_virt.id in (:tmIds)
          left join translation_memory_project tmp_recv
            on tmp_recv.translation_memory_id = tm_virt.id
           and tmp_recv.project_id = :projectId
          left join branch b on b.id = k.branch_id
          join translation target_t
            on target_t.key_id = k.id
           and target_t.language_id = inputs.target_lang_id
          where base_t.text = inputs.base_text
            and target_t.text is not null and target_t.text <> ''
            and (b.id is null or b.is_default = true)
            and (not tm_virt.write_only_reviewed or target_t.state = 2)
            and coalesce(tmp_recv.penalty, tm_virt.default_penalty) = 0
        ) candidates
        order by case kind when 'stored' then 0 else 1 end
        limit 1
      ) m
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("reqKeyIds", reqKeyIds)
        .setParameter("reqLangIds", reqLangIds)
        .setParameter("tmIds", tmIds)
        .setParameter("projectId", projectId)
        .resultList as List<Array<Any?>>

    val result = HashMap<Pair<Long, Long>, TranslationMemoryItemView>(rows.size)
    rows.forEach { row ->
      val reqKeyId = (row[0] as Number).toLong()
      val targetLangId = (row[1] as Number).toLong()
      val pair = reqKeyId to targetLangId
      val targetText = row[2] as String
      val baseText = row[3] as String
      // Match the per-item EE impl: surface request-key identity on the view (keyName/keyId
      // refer to the key being translated, not the contributing key) so the caller's
      // `targetTranslationText`-based usage stays identical.
      val requestKey = keysById[reqKeyId] ?: return@forEach
      result[pair] =
        TranslationMemoryItemView(
          baseTranslationText = baseText,
          targetTranslationText = targetText,
          keyName = requestKey.name,
          keyNamespace = null,
          similarity = 1.0f,
          keyId = requestKey.id,
        )
    }
    return result
  }

  private fun findExactMatch(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val baseText = translationService.findBaseTranslation(key)?.text ?: return null
    val tmIds =
      translationMemoryManagementService.getReadableTmIdsForSuggestions(
        projectId = key.project.id,
        organizationId = key.project.organizationOwner.id,
      )
    if (tmIds.isEmpty()) return null

    // Two-half UNION ALL:
    //   1. Stored entries on any readable TM with an exact source-text match.
    //   2. Virtual rows — exact-text matches against translations on any project that has
    //      writeAccess to a readable TM. Mirrors the virtual-row half of
    //      `TranslationMemoryServiceEeImpl.baseSelect`, just with `=` instead of trigram `%`.
    //
    // Both halves enforce the same penalty gate: the receiving project's assignment to the
    // contributing TM (`tmp_recv`) must resolve to an effective penalty of 0 — `coalesce(
    // tmp_recv.penalty, tm.default_penalty) = 0`. Anything non-zero is "the user marked this
    // TM as not fully trusted" and shouldn't drive auto-fills.
    //
    // The `kind` column lets us pick stored entries first when both halves return rows,
    // mirroring how the suggestion panel surfaces a user's manual additions.
    val sql =
      """
      select target_text, kind from (
        select e.target_text, 'stored' as kind
        from translation_memory_entry e
        join translation_memory tm on tm.id = e.translation_memory_id
        left join translation_memory_project tmp_recv
          on tmp_recv.translation_memory_id = e.translation_memory_id
         and tmp_recv.project_id = :projectId
        where e.translation_memory_id in :tmIds
          and e.source_text = :baseText
          and e.target_language_tag = :targetLanguageTag
          and e.target_text <> ''
          and coalesce(tmp_recv.penalty, tm.default_penalty) = 0

        union all

        select target_t.text as target_text, 'virtual' as kind
        from translation base_t
        join key k on k.id = base_t.key_id and k.deleted_at is null
        join project p
          on p.id = k.project_id
         and p.base_language_id = base_t.language_id
         and p.deleted_at is null
        join translation_memory_project tmp_w
          on tmp_w.project_id = p.id and tmp_w.write_access = true
        join translation_memory tm_virt
          on tm_virt.id = tmp_w.translation_memory_id
         and tm_virt.id in :tmIds
        left join translation_memory_project tmp_recv
          on tmp_recv.translation_memory_id = tm_virt.id
         and tmp_recv.project_id = :projectId
        left join branch b on b.id = k.branch_id
        join translation target_t
          on target_t.key_id = k.id
         and target_t.language_id <> base_t.language_id
        join language target_lang on target_lang.id = target_t.language_id
        where base_t.text = :baseText
          and target_lang.tag = :targetLanguageTag
          and target_t.text is not null and target_t.text <> ''
          and (b.id is null or b.is_default = true)
          and (not tm_virt.write_only_reviewed or target_t.state = 2)
          and k.id <> :selfKeyId
          and coalesce(tmp_recv.penalty, tm_virt.default_penalty) = 0
      ) candidates
      order by case kind when 'stored' then 0 else 1 end
      limit 1
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmIds", tmIds)
        .setParameter("baseText", baseText)
        .setParameter("targetLanguageTag", targetLanguage.tag)
        .setParameter("selfKeyId", key.id)
        .setParameter("projectId", key.project.id)
        .resultList as List<Array<Any?>>
    val row = rows.firstOrNull() ?: return null
    return TranslationMemoryItemView(
      baseTranslationText = baseText,
      targetTranslationText = row[0] as String,
      keyName = key.name,
      keyNamespace = null,
      similarity = 1.0f,
      keyId = key.id,
    )
  }
}
