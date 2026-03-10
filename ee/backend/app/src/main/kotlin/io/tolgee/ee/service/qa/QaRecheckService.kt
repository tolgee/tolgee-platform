package io.tolgee.ee.service.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
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
  ) {
    val translationIds = translationService.getTranslationIdsForRecheck(projectId, languageIds)
    if (translationIds.isEmpty()) return

    translationService.setQaChecksStale(translationIds)

    batchJobService.startJob(
      request = QaCheckRequest(translationIds = translationIds, checkTypes = checkTypes),
      project = entityManager.getReference(Project::class.java, projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
