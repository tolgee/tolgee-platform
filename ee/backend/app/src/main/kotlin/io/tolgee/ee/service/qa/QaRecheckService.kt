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

    val target = pairs.map { BatchTranslationTargetItem(keyId = it.keyId, languageId = it.languageId) }

    batchJobService.startJob(
      request = QaCheckRequest(target = target, checkTypes = checkTypes),
      project = entityManager.getReference(Project::class.java, projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
