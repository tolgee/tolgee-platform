package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationTextsModified
import io.tolgee.model.Project
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTextsModifiedListener(
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val projectService: ProjectService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationTextsModified(event: OnTranslationTextsModified) {
    if (event.translationIds.isEmpty()) return

    val project = projectService.getDto(event.projectId)
    if (!enabledFeaturesProvider.isFeatureEnabled(project.organizationOwnerId, Feature.QA_CHECKS)) return

    batchJobService.startJob(
      request = QaCheckRequest(translationIds = event.translationIds),
      project = entityManager.getReference(Project::class.java, event.projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
