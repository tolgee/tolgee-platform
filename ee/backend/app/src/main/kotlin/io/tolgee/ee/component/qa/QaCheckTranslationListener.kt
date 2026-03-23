package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationsSet
import io.tolgee.service.project.ProjectFeatureGuard
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTranslationListener(
  private val batchJobService: BatchJobService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationsSet(event: OnTranslationsSet) {
    val translationIds = event.translations.map { it.id }
    if (translationIds.isEmpty()) return

    val project = event.key.project
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    batchJobService.startJob(
      request = QaCheckRequest(translationIds = translationIds),
      project = project,
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
