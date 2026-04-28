package io.tolgee.ee.component.translationMemory

import io.sentry.Sentry
import io.tolgee.constants.Feature
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.service.project.ProjectService
import io.tolgee.service.translationMemory.TranslationMemoryManagementService
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Creates project-type Translation Memory rows for projects in an organization when the
 * TRANSLATION_MEMORY feature is gained (e.g. plan upgrade). Project TMs are pure config (name,
 * default penalty, writeOnlyReviewed, priority); their content is computed virtually from the
 * project's translations at read time, so there is no data backfill involved.
 *
 * Projects created before the TM management feature did not get a project TM row on creation;
 * this listener brings them up to date so the TM content browser and suggestion path find an
 * existing [TranslationMemory] row without a lazy-provision detour on first access.
 */
@Component
class TranslationMemoryFeatureListener(
  private val projectService: ProjectService,
  private val translationMemoryManagementService: TranslationMemoryManagementService,
) {
  private val logger = logger()

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onOrganizationFeaturesChanged(event: OnOrganizationFeaturesChanged) {
    if (Feature.TRANSLATION_MEMORY !in event.gainedFeatures) return

    val orgId = event.organizationId
    val projectIds =
      if (orgId != null) {
        projectService.findAllActiveInOrganization(orgId).map { it.id }
      } else {
        projectService.findAllActive().map { it.id }
      }
    if (projectIds.isEmpty()) return

    logger.info(
      "Ensuring project TM exists for ${projectIds.size} project(s) after TRANSLATION_MEMORY feature gained" +
        (orgId?.let { " in organization $it" } ?: ""),
    )

    projectIds.forEach { projectId ->
      try {
        translationMemoryManagementService.getOrCreateProjectTm(projectId)
      } catch (e: Exception) {
        logger.error("Failed to create project TM for project $projectId", e)
        Sentry.captureException(e)
      }
    }
  }
}
