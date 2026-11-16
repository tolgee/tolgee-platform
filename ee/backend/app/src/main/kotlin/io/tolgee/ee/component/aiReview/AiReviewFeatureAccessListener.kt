package io.tolgee.ee.component.aiReview

import io.sentry.Sentry
import io.tolgee.constants.Feature
import io.tolgee.ee.service.aiReview.AiReviewQueueService
import io.tolgee.events.OnOrganizationFeaturesChanged
import io.tolgee.repository.aiReview.TranslationAiReviewRepository
import io.tolgee.util.logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AiReviewFeatureAccessListener(
  private val reviewRepository: TranslationAiReviewRepository,
  private val aiReviewQueueService: AiReviewQueueService,
) {
  private val logger = logger()

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  fun onOrganizationFeaturesChanged(event: OnOrganizationFeaturesChanged) {
    if (Feature.AI_REVIEW !in event.gainedFeatures) return
    val projectIds =
      event.organizationId?.let { reviewRepository.findProjectIdsWithClaimableRows(it) }
        ?: reviewRepository.findProjectIdsWithClaimableRows()
    if (projectIds.isEmpty()) return
    logger.info("Starting AI review self-heal for ${projectIds.size} project(s) after feature regain")
    projectIds.forEach { projectId ->
      try {
        aiReviewQueueService.selfHeal(projectId)
      } catch (e: Exception) {
        logger.error("Failed to self-heal AI reviews for project $projectId", e)
        Sentry.captureException(e)
      }
    }
  }
}
