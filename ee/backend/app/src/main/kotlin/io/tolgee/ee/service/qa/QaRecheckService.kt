package io.tolgee.ee.service.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.data.BatchTranslationTargetItem
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.model.Project
import io.tolgee.model.enums.qa.QaCheckType
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class QaRecheckService(
  private val batchJobService: BatchJobService,
  private val translationService: TranslationService,
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

    val pairs = translationService.getKeyLanguagePairsForRecheck(projectId, languageIds, onlyStale)
    if (pairs.isEmpty()) return

    if (!onlyStale) {
      val existingTranslationIds =
        translationService.getNotStaleTranslationIds(projectId, languageIds)
      if (existingTranslationIds.isNotEmpty()) {
        translationService.setQaChecksStale(existingTranslationIds)
      }
    }

    val allTargetItems = pairs.map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }
    val project = entityManager.getReference(Project::class.java, projectId)

    for (targetChunk in allTargetItems.chunked(MAX_BATCH_JOB_TARGET_SIZE)) {
      batchJobService.startJob(
        request = QaCheckRequest(target = targetChunk, checkTypes = checkTypes),
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
    const val MAX_BATCH_JOB_TARGET_SIZE = 10_000
  }
}
