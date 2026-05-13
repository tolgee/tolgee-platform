package io.tolgee.service.translation

import io.tolgee.model.key.Key
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import java.util.Date

/**
 * Common scaffolding for [TranslationMemoryService] implementations: parameter binding, similarity
 * threshold setup, row mapping, and the [getSuggestions] orchestration. Subclasses supply the
 * inner SQL — OSS provides a virtual-only variant; EE overrides with a full UNION over stored
 * entries.
 */
abstract class AbstractTranslationMemoryService(
  protected val entityManager: EntityManager,
  protected val translationMemoryManagementService: TranslationMemoryManagementService,
) : TranslationMemoryService {
  /**
   * SQL fragment producing the raw `(targetTranslationText, baseTranslationText, …)` rows the
   * deduper consumes. Implementations differ in whether they include stored TM entries.
   */
  protected abstract val baseSelect: String

  protected val dedupedBaseSelect: String
    get() =
      """
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
      from ($baseSelect) raw
      order by baseTranslationText, targetTranslationText,
               similarity desc, assignmentPriority asc
      """

  @Transactional
  override fun getSuggestions(
    key: Key,
    targetLanguageTag: String,
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
      targetLanguageTag = targetLanguageTag,
      pageable = pageable,
    )
  }

  @Transactional
  override fun getSuggestions(
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
    if (tmIds.isEmpty()) return Page.empty(pageable)

    setSimilarityThreshold()
    val resultList =
      suggestionsQuery(
        sql =
          """
          with deduped as ($dedupedBaseSelect)
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

    // count(*) over() trails the deduped projection; deduped has 10 columns
    // (indexes 0..9), so the window count is at index 10. PG returns it as bigint —
    // JDBC may surface Long or BigInteger depending on driver, so go through Number.
    val count = ((resultList.firstOrNull() as Array<*>?)?.get(10) as Number?)?.toLong() ?: 0L
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
  override fun getSuggestionsList(
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
          select * from ($dedupedBaseSelect) deduped
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
      updatedAt = row[9] as Date?,
    )
}
