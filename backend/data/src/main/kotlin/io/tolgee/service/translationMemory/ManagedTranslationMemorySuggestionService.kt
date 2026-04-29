package io.tolgee.service.translationMemory

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Suggestion service for the new Translation Memory system. Queries `translation_memory_entry`
 * (separate from the legacy `TranslationMemoryService` which queries the `translation` table).
 *
 * Plan-aware via [TranslationMemoryManagementService.getReadableTmIdsForSuggestions]:
 *   - Free plan → only the project's own PROJECT-type TM is included.
 *   - Paid plan (`Feature.TRANSLATION_MEMORY`) → project TM + every assigned shared TM with read access.
 *
 * The underlying content comes from three sources, unioned in [BASE_SELECT]:
 *   1. Stored shared-TM entries (both synced and manual).
 *   2. Stored manual entries in the project's own PROJECT TM.
 *   3. Virtual rows computed on the fly from the current project's translations for any PROJECT
 *      TM in [tmIds] — entries in a project TM are not materialized, so the suggestion path
 *      derives them each query.
 *
 * If a project has no readable TMs at all (existing free-plan project that pre-dates the feature),
 * the caller should fall through to the legacy `TranslationMemoryService`.
 */
@Service
class ManagedTranslationMemorySuggestionService(
  private val entityManager: EntityManager,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
) {
  @Transactional
  fun getSuggestions(
    key: Key,
    targetLanguage: LanguageDto,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val project = key.project
    val baseLanguage = project.baseLanguage ?: return Page.empty(pageable)
    val baseTranslation =
      key.translations.firstOrNull { it.language.id == baseLanguage.id }
    val baseText = baseTranslation?.text ?: return Page.empty(pageable)

    return getSuggestions(
      baseTranslationText = baseText,
      isPlural = key.isPlural,
      keyId = key.id,
      projectId = project.id,
      organizationId = project.organizationOwner.id,
      targetLanguageTag = targetLanguage.tag,
      pageable = pageable,
    )
  }

  @Transactional
  fun getSuggestions(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    projectId: Long,
    organizationId: Long,
    targetLanguageTag: String,
    pageable: Pageable,
  ): Page<TranslationMemoryItemView> {
    val tmIds =
      translationMemoryManagementService.getReadableTmIdsForSuggestions(projectId, organizationId)
    // Guard required: the SQL below uses `in :tmIds` which PostgreSQL rejects on an empty
    // collection. Returning early also avoids an unnecessary round trip.
    if (tmIds.isEmpty()) return Page.empty(pageable)

    setSimilarityThreshold()
    val resultList =
      suggestionsQuery(
        sql =
          """
          with deduped as ($DEDUPED_BASE_SELECT)
          select deduped.*, count(*) over()
          from deduped
          order by deduped.similarity desc, deduped.assignmentPriority asc
          """.trimIndent(),
        baseTranslationText = baseTranslationText,
        isPlural = isPlural,
        keyId = keyId,
        projectId = projectId,
        tmIds = tmIds,
        targetLanguageTag = targetLanguageTag,
      ).setMaxResults(pageable.pageSize)
        .setFirstResult(pageable.offset.toInt())
        .resultList

    // count(*) over() trails the deduped projection; deduped now has 10 columns
    // (indexes 0..9), so the window count is at index 10.
    val count = (resultList.firstOrNull() as Array<*>?)?.get(10) as Long? ?: 0L
    return PageImpl(resultList.map { mapRow(it as Array<*>) }, pageable, count)
  }

  /**
   * Non-paginated variant used by the MT pipeline ([io.tolgee.service.machineTranslation.MetadataProvider])
   * to enrich provider prompts with up to [limit] similar prior translations.
   *
   * Differences vs [getSuggestions]:
   * - Returns a raw [List] — no pagination, no count window.
   * - Sorted by penalized similarity so trust-adjusted examples win.
   * - No `REQUIRES_NEW` — MT batches fan chunks out in parallel and wrapping every TM
   *   lookup in a nested transaction would double the connection-pool footprint.
   *   The only `SET LOCAL` we issue (`pg_trgm.similarity_threshold`) is benign in
   *   the caller's transaction because no other similarity-sensitive query follows
   *   in the same request.
   */
  @Transactional
  fun getSuggestionsList(
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    projectId: Long,
    organizationId: Long,
    targetLanguageTag: String,
    limit: Int,
  ): List<TranslationMemoryItemView> {
    val tmIds =
      translationMemoryManagementService.getReadableTmIdsForSuggestions(projectId, organizationId)
    if (tmIds.isEmpty()) return emptyList()

    setSimilarityThreshold()
    val resultList =
      suggestionsQuery(
        sql =
          """
          select * from ($DEDUPED_BASE_SELECT) deduped
          order by deduped.similarity desc, deduped.assignmentPriority asc
          """.trimIndent(),
        baseTranslationText = baseTranslationText,
        isPlural = isPlural,
        keyId = keyId,
        projectId = projectId,
        tmIds = tmIds,
        targetLanguageTag = targetLanguageTag,
      ).setMaxResults(limit).resultList

    return resultList.map { mapRow(it as Array<*>) }
  }

  private fun setSimilarityThreshold() {
    entityManager.createNativeQuery("set local pg_trgm.similarity_threshold to 0.5").executeUpdate()
  }

  private fun suggestionsQuery(
    sql: String,
    baseTranslationText: String,
    isPlural: Boolean,
    keyId: Long?,
    projectId: Long,
    tmIds: List<Long>,
    targetLanguageTag: String,
  ): Query =
    entityManager
      .createNativeQuery(sql)
      .setParameter("baseTranslationText", baseTranslationText)
      .setParameter("isPlural", isPlural)
      .setParameter("keyId", keyId)
      .setParameter("projectId", projectId)
      .setParameter("tmIds", tmIds)
      .setParameter("targetLanguageTag", targetLanguageTag)

  private fun mapRow(row: Array<*>): TranslationMemoryItemView =
    TranslationMemoryItemView(
      targetTranslationText = row[0] as String,
      baseTranslationText = row[1] as String,
      keyName = (row[2] as String?) ?: "",
      keyNamespace = row[3] as String?,
      keyId = (row[4] as Number).toLong(),
      rawSimilarity = (row[5] as Number).toFloat(),
      similarity = (row[6] as Number).toFloat(),
      translationMemoryName = row[7] as String?,
      // index 8 is assignmentPriority (kept for ordering only — not exposed)
      updatedAt = row[9] as java.util.Date?,
    )

  companion object {
    /**
     * Raw match rows, unioned across the three entry sources:
     *
     * 1. **Stored entries** (shared TMs of any kind, plus manual entries in project TMs).
     *    Key name comes from any one contributing translation via `translation_memory_entry_source`
     *    — for synced rows — or is NULL for manual rows.
     * 2. **Virtual entries** — computed on the fly for each project TM in `:tmIds` from the
     *    current project's translations. Key name is the translation's key directly. Filtered
     *    by default branch, non-empty text, and `writeOnlyReviewed` state if set on the TM.
     *
     * The `:keyId` exclusion runs independently in each branch using the schema available there
     * (join-table lookup for stored synced entries, direct `k.id` comparison for virtual).
     */
    private const val BASE_SELECT = """
      select re.target_text as targetTranslationText,
             re.source_text as baseTranslationText,
             re.key_name as keyName,
             re.key_namespace as keyNamespace,
             re.key_id as keyId,
             similarity(re.source_text, :baseTranslationText) as rawSimilarity,
             case
               when tm.type = 'PROJECT' then similarity(re.source_text, :baseTranslationText)
               else greatest(
                 similarity(re.source_text, :baseTranslationText)
                   - (coalesce(tmp.penalty, tm.default_penalty) / 100.0),
                 0
               )
             end as similarity,
             tm.name as translationMemoryName,
             tmp.priority as assignmentPriority,
             re.updated_at as updatedAt
      from (
        -- Stored entries: shared TMs (both synced and manual) + manual rows in project TMs.
        -- Key name is aggregated from any one contributing translation's key for synced entries;
        -- manual entries (no source rows) leave it null.
        select tme.target_text,
               tme.source_text,
               tme.target_language_tag,
               tme.translation_memory_id as tm_id,
               kn.key_name,
               kn.key_namespace,
               coalesce(kn.key_id, 0) as key_id,
               kn.any_key_is_plural as any_key_is_plural,
               kn.includes_current_key as includes_current_key,
               tme.updated_at as updated_at
        from translation_memory_entry tme
        join translation_memory tm_src on tm_src.id = tme.translation_memory_id
        left join lateral (
          select min(k.name) as key_name,
                 min(ns.name) as key_namespace,
                 min(k.id) as key_id,
                 bool_or(k.is_plural) as any_key_is_plural,
                 bool_or(cast(:keyId as bigint) is not null and k.id = :keyId) as includes_current_key
          from translation_memory_entry_source s
          join translation t on t.id = s.translation_id
          join key k on k.id = t.key_id
          left join namespace ns on ns.id = k.namespace_id
          where s.entry_id = tme.id
        ) kn on true
        where tme.translation_memory_id in :tmIds
          and tme.target_language_tag = :targetLanguageTag
          and tme.source_text % :baseTranslationText
          and (tm_src.type <> 'PROJECT' or tme.is_manual = true)

        union all

        -- Virtual entries: computed from the project's translations for every PROJECT-type TM
        -- in :tmIds. We still emit the tm_id so the outer select can apply tm-level config
        -- (penalty, priority, name) from the matching translation_memory row. updated_at comes
        -- from the contributing target translation — that's what users perceive as the "match
        -- age" for a virtual row, since the row materialises from the translation.
        --
        -- Driven by a trigram-filtered subquery on `translation` so the planner is forced to
        -- use `translation_lang_text_gist_trgm` (language_id, text) before any other join.
        -- Joining `key` and target translations off that small candidate set keeps the cost
        -- proportional to the number of matched rows, not to the project's total key count.
        select
          target_t.text as target_text,
          base_match.text as source_text,
          target_lang.tag as target_language_tag,
          tm_virt.id as tm_id,
          k.name as key_name,
          ns.name as key_namespace,
          k.id as key_id,
          k.is_plural as any_key_is_plural,
          (cast(:keyId as bigint) is not null and k.id = :keyId) as includes_current_key,
          target_t.updated_at as updated_at
        from (
          select base_t.key_id, base_t.text, base_t.language_id
          from translation base_t
          where base_t.language_id =
                (select p.base_language_id from project p where p.id = :projectId)
            and base_t.text <> ''
            and base_t.text is not null
            and base_t.text % :baseTranslationText
        ) base_match
        join key k on k.id = base_match.key_id and k.project_id = :projectId
        left join namespace ns on ns.id = k.namespace_id
        left join branch b on b.id = k.branch_id
        join translation target_t
          on target_t.key_id = k.id and target_t.language_id <> base_match.language_id
        join language target_lang on target_lang.id = target_t.language_id
        join translation_memory tm_virt
          on tm_virt.id in :tmIds and tm_virt.type = 'PROJECT'
        where target_lang.tag = :targetLanguageTag
          and target_t.text is not null and target_t.text <> ''
          and (b.id is null or b.is_default = true)
          and (not tm_virt.write_only_reviewed or target_t.state = 2)
      ) re
      join translation_memory tm on tm.id = re.tm_id
      left join translation_memory_project tmp
        on tmp.translation_memory_id = re.tm_id
        and tmp.project_id = :projectId
      where (cast(:keyId as bigint) is null or not coalesce(re.includes_current_key, false))
        and (re.any_key_is_plural is null or re.any_key_is_plural = :isPlural)
    """

    /** Collapses identical (source, target) rows from multiple TMs; highest similarity then lowest priority wins. */
    private const val DEDUPED_BASE_SELECT = """
      select distinct on (baseTranslationText, targetTranslationText)
             targetTranslationText,
             baseTranslationText,
             keyName,
             keyNamespace,
             keyId,
             rawSimilarity,
             similarity,
             translationMemoryName,
             assignmentPriority,
             updatedAt
      from ($BASE_SELECT) raw
      order by baseTranslationText, targetTranslationText,
               similarity desc, assignmentPriority asc
    """
  }
}
