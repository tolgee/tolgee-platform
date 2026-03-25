package io.tolgee.ee.component.qa

import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.QaRecheckService
import io.tolgee.events.OnLanguageTagChanged
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaLanguageTagChangeListener(
  private val qaRecheckService: QaRecheckService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  fun onLanguageTagChanged(event: OnLanguageTagChanged) {
    val project = projectService.getDto(event.projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    qaRecheckService.recheckTranslations(
      projectId = event.projectId,
      languageIds = listOf(event.languageId),
    )
  }
}
