package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.events.OnTranslationsSet
import io.tolgee.model.Project
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTranslationListener(
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationsSet(event: OnTranslationsSet) {
    val translationIds = event.translations.map { it.id }
    if (translationIds.isEmpty()) return

    val projectId = event.key.project.id
    batchJobService.startJob(
      request = QaCheckRequest(translationIds = translationIds),
      project = entityManager.getReference(Project::class.java, projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
