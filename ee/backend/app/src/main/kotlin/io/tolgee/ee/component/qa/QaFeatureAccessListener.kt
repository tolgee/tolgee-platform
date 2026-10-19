package io.tolgee.ee.component.qa

import io.sentry.Sentry
import io.tolgee.constants.Feature
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.model.Project
import io.tolgee.service.organization.OrganizationFeatureGrantChangeService
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
  private val organizationFeatureGrantChangeService: OrganizationFeatureGrantChangeService,
) {
  private val logger = logger()

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onOrganizationFeaturesChanged(event: OnOrganizationFeaturesChanged) {
    // Disable on both gain AND loss: gaining the feature would make stale QA flags visible
    // without a recheck; this way we get two chances to disable QA checks for the project.
    // User must manually re-enable QA, which triggers a fresh recheck.
    val qaChecksAffected = Feature.QA_CHECKS in event.gainedFeatures + event.lostFeatures
    if (!qaChecksAffected) return

    val orgId = event.organizationId
    val candidates = findQaEnabledProjects(orgId)
    val projects =
      candidates.filter {
        Feature.QA_CHECKS in
          organizationFeatureGrantChangeService.getEffectivelyChangedFeatures(event, it.organizationOwner.id)
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

  private fun findQaEnabledProjects(organizationId: Long?): List<Project> {
    if (organizationId != null) return projectService.findAllWithQaEnabledInOrganization(organizationId)
    return projectService.findAllWithQaEnabled()
  }
}
