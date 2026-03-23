package io.tolgee.ee.component.qa

import io.sentry.Sentry
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.service.project.ProjectService
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QaFeatureAccessListener(
  private val projectService: ProjectService,
  private val projectQaConfigService: ProjectQaConfigService,
) {
  private val logger = logger()

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onOrganizationFeaturesChanged(event: OnOrganizationFeaturesChanged) {
    // Disable on both gain AND loss: gaining the feature would make stale QA flags visible
    // without a recheck; this way we get to chances to disable QA checks for the project.
    // User must manually re-enable QA, which triggers a fresh recheck.
    if (Feature.QA_CHECKS !in event.gainedFeatures && Feature.QA_CHECKS !in event.lostFeatures) return

    val orgId = event.organizationId
    val projects =
      if (orgId != null) {
        projectService.findAllWithQaEnabledInOrganization(orgId)
      } else {
        projectService.findAllWithQaEnabled()
      }
    if (projects.isEmpty()) return

    logger.info(
      "Disabling QA checks for ${projects.size} project(s) due to feature access change" +
        (orgId?.let { " in organization $it" } ?: ""),
    )

    projects.forEach { project ->
      try {
        projectQaConfigService.setQaEnabled(project.id, false)
      } catch (e: Exception) {
        logger.error("Failed to disable QA checks for project ${project.id}", e)
        Sentry.captureException(e)
      }
    }
  }
}
