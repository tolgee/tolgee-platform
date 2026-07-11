package io.tolgee.ee.service.qa

import io.sentry.Sentry
import io.sentry.SentryLevel
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.dtos.queryResults.qa.KeyLanguagePairView
import io.tolgee.model.Project
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.service.project.ProjectFeatureRegistry
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaRecheckService(
  private val batchJobService: BatchJobService,
  private val translationService: TranslationService,
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
  @Lazy private val projectQaConfigService: ProjectQaConfigService,
) {
  @Transactional
  fun recheckTranslations(
    projectId: Long,
    checkTypes: List<QaCheckType>? = null,
    languageIds: List<Long>? = null,
    onlyStale: Boolean = false,
  ) {
    if (checkTypes?.isEmpty() == true) return

    if (!onlyStale) {
      markStaleForRecheck(projectId, checkTypes, languageIds)
    }

    val enabledLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)
    val activeLanguageIds = languageIds?.filter { it in enabledLanguageIds } ?: enabledLanguageIds

    val pairs = translationService.getKeyLanguagePairsForQaRecheck(projectId, activeLanguageIds, onlyStale)
    if (pairs.isEmpty()) return

    recheckFromKeyLanguagePairs(projectId, pairs, checkTypes = checkTypes)
  }

  private fun markStaleForRecheck(
    projectId: Long,
    checkTypes: List<QaCheckType>?,
    languageIds: List<Long>?,
  ) {
    if (checkTypes == null) {
      if (languageIds == null) {
        translationService.setQaChecksStaleByProjectId(projectId)
        return
      }
      translationService.setQaChecksStaleByProjectIdAndLanguageIds(projectId, languageIds)
      return
    }
    // QA-disabled languages are excluded from the recheck below; marking them stale defers
    // their recheck to when QA is re-enabled for them (which only reprocesses stale ones).
    val disabledLanguageIds = projectQaConfigService.getDisabledLanguageIds(projectId)
    val disabledInScope = languageIds?.filter { it in disabledLanguageIds } ?: disabledLanguageIds.toList()
    if (disabledInScope.isEmpty()) return
    translationService.setQaChecksStaleByProjectIdAndLanguageIds(projectId, disabledInScope)
  }

  @Transactional
  fun recheckStaleTranslationsOnBranch(
    projectId: Long,
    branchId: Long,
  ) {
    val projectDto = projectService.getDto(projectId)
    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, projectDto)) return

    val enabledLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)
    val pairs =
      translationService.getStaleKeyLanguagePairsByBranchAndLanguageIds(
        projectId,
        branchId,
        enabledLanguageIds,
      )
    if (pairs.isEmpty()) return

    recheckFromKeyLanguagePairs(projectId, pairs)
  }

  @Transactional
  fun recheckStuckStaleTranslationsInProject(projectId: Long) {
    val projectDto = projectService.getDto(projectId)
    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, projectDto)) return
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.QA_CHECK)) return

    val enabledLanguageIds = projectQaConfigService.getEnabledLanguageIds(projectId)
    val pairs = translationService.getStaleKeyLanguagePairsByProjectAndLanguageIds(projectId, enabledLanguageIds)
    if (pairs.isEmpty()) return

    Sentry.captureMessage(
      "Found ${pairs.size} stuck-stale translation(s) after QA batch job finished; " +
        "enqueueing self-heal QA recheck. projectId=$projectId",
      SentryLevel.WARNING,
    )

    recheckFromKeyLanguagePairs(projectId, pairs, isHandlingStuckStaleItems = true)
  }

  private fun recheckFromKeyLanguagePairs(
    projectId: Long,
    pairs: List<KeyLanguagePairView>,
    checkTypes: List<QaCheckType>? = null,
    isHandlingStuckStaleItems: Boolean = false,
  ) {
    val allTargetItems = pairs.map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }
    val project = entityManager.getReference(Project::class.java, projectId)

    for (targetChunk in allTargetItems.chunked(MAX_BATCH_JOB_TARGET_SIZE)) {
      batchJobService.startJob(
        request =
          QaCheckRequest(
            target = targetChunk,
            checkTypes = checkTypes,
            handlingStuckStaleItems = isHandlingStuckStaleItems,
          ),
        project = project,
        author = null,
        type = BatchJobType.QA_CHECK,
        isHidden = true,
      )
    }
  }

  companion object {
    /**
     * Maximum number of target items per batch job to prevent large JSONB payloads
     * in the batch_job table and excessive memory usage during serialization.
     */
    const val MAX_BATCH_JOB_TARGET_SIZE = 32_000
  }
}
