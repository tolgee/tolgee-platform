package io.tolgee.ee.component.qa

import io.sentry.Sentry
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.QaRecheckService
import io.tolgee.events.OnLanguageTagChanged
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaLanguageTagChangeListener(
  private val qaRecheckService: QaRecheckService,
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
) {
  private val logger = logger()

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onLanguageTagChanged(event: OnLanguageTagChanged) {
    val project = projectService.getDto(event.projectId)
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return

    try {
      qaRecheckService.recheckTranslations(
        projectId = event.projectId,
        languageIds = listOf(event.languageId),
      )
    } catch (e: Exception) {
      logger.error("Failed to recheck QA after language tag change for project ${event.projectId}", e)
      Sentry.captureException(e)
    }
  }
}
