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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaRecheckService(
  private val batchJobService: BatchJobService,
  private val translationService: TranslationService,
  private val projectService: ProjectService,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun recheckTranslations(
    projectId: Long,
    checkTypes: List<QaCheckType>? = null,
    languageIds: List<Long>? = null,
    onlyStale: Boolean = false,
  ) {
    if (checkTypes?.isEmpty() == true) return

    val pairs = translationService.getKeyLanguagePairsForQaRecheck(projectId, languageIds, onlyStale)
    if (pairs.isEmpty()) return

    if (checkTypes == null && !onlyStale) {
      if (languageIds == null) {
        translationService.setQaChecksStaleByProjectId(projectId)
      } else {
        translationService.setQaChecksStaleByProjectIdAndLanguageIds(projectId, languageIds)
      }
    }

    recheckFromKeyLanguagePairs(projectId, pairs, checkTypes = checkTypes)
  }

  @Transactional
  fun recheckStaleTranslationsOnBranch(
    projectId: Long,
    branchId: Long,
  ) {
    val projectDto = projectService.getDto(projectId)
    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, projectDto)) return

    val pairs = translationService.getStaleKeyLanguagePairsByBranch(projectId, branchId)
    if (pairs.isEmpty()) return

    recheckFromKeyLanguagePairs(projectId, pairs)
  }

  @Transactional
  fun recheckStuckStaleTranslationsInProject(projectId: Long) {
    val projectDto = projectService.getDto(projectId)
    if (!ProjectFeatureRegistry.isEnabledOnProject(Feature.QA_CHECKS, projectDto)) return
    if (batchJobService.hasActiveJobForProject(projectId, BatchJobType.QA_CHECK)) return

    val pairs = translationService.getStaleKeyLanguagePairsByProject(projectId)
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
