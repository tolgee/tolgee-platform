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

/**
 * EE auto-translate resolver. Extends the OSS exact-equality lookup with a prior pass over
 * stored TM entries (manual entries + TMX imports) on every TM the project can read. Both
 * passes use exact equality on source text — the composite `(translation_memory_id,
 * source_text)` index covers it — so the per-key latency stays in the millisecond range
 * that the batch / MT pipelines require.
 *
 * Plan-aware filtering is handled by [TranslationMemoryManagementService.getReadableTmIdsForSuggestions]:
 * when the feature is disabled it returns only the project's own TM, so this override
 * degrades gracefully on free-tier orgs without behaving differently from the OSS path
 * (project-type TMs rarely carry stored entries).
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
      val result = findStoredEntryMatch(key, targetLanguage)
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

  private fun findStoredEntryMatch(
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

    val sql =
      """
      select e.source_text, e.target_text
      from translation_memory_entry e
      where e.translation_memory_id in :tmIds
        and e.source_text = :baseText
        and e.target_language_tag = :targetLanguageTag
        and e.target_text <> ''
      limit 1
      """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val rows =
      entityManager
        .createNativeQuery(sql)
        .setParameter("tmIds", tmIds)
        .setParameter("baseText", baseText)
        .setParameter("targetLanguageTag", targetLanguage.tag)
        .resultList as List<Array<Any?>>
    val row = rows.firstOrNull() ?: return null
    return TranslationMemoryItemView(
      baseTranslationText = row[0] as String,
      targetTranslationText = row[1] as String,
      keyName = key.name,
      keyNamespace = null,
      similarity = 1.0f,
      keyId = key.id,
    )
  }
}
