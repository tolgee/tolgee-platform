package io.tolgee.ee.component.qa

import io.tolgee.batch.BatchJobService
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.QaCheckRequest
import io.tolgee.constants.Feature
import io.tolgee.events.OnTranslationTextsModified
import io.tolgee.model.Project
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaCheckTextsModifiedListener(
  private val batchJobService: BatchJobService,
  private val entityManager: EntityManager,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val translationService: TranslationService,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onTranslationTextsModified(event: OnTranslationTextsModified) {
    if (event.translationIds.isEmpty()) return

    val project = projectService.getDto(event.projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    val baseLanguage = languageService.getProjectBaseLanguage(event.projectId)
    val siblingIds =
      translationService.getSiblingIdsForBaseLanguageChanges(
        translationIds = event.translationIds,
        baseLanguageId = baseLanguage.id,
      )

    if (siblingIds.isNotEmpty()) {
      translationService.setQaChecksStale(siblingIds)
    }

    batchJobService.startJob(
      request = QaCheckRequest(translationIds = event.translationIds + siblingIds),
      project = entityManager.getReference(Project::class.java, event.projectId),
      author = null,
      type = BatchJobType.QA_CHECK,
      isHidden = true,
    )
  }
}
