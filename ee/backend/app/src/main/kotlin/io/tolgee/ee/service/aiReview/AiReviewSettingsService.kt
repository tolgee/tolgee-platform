package io.tolgee.ee.service.aiReview

import io.tolgee.ee.data.aiReview.AiReviewSettingsRequest
import io.tolgee.model.Project
import io.tolgee.model.aiReview.AiReviewProjectSettings
import io.tolgee.repository.aiReview.AiReviewProjectSettingsRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Service
class AiReviewSettingsService(
  private val aiReviewProjectSettingsRepository: AiReviewProjectSettingsRepository,
  private val entityManager: EntityManager,
) {
  fun find(projectId: Long): AiReviewProjectSettings? =
    aiReviewProjectSettingsRepository.findById(projectId).getOrNull()

  fun get(projectId: Long): AiReviewProjectSettings = find(projectId) ?: AiReviewProjectSettings(projectId = projectId)

  @Transactional
  fun update(
    projectId: Long,
    request: AiReviewSettingsRequest,
  ): AiReviewProjectSettings {
    val settings =
      find(projectId)
        ?: AiReviewProjectSettings(project = entityManager.getReference(Project::class.java, projectId))
    settings.onSourceChangeAiApproved = request.onSourceChangeAiApproved
    settings.onSourceChangeHumanEdited = request.onSourceChangeHumanEdited
    settings.onSourceChangeHumanReviewed = request.onSourceChangeHumanReviewed
    return aiReviewProjectSettingsRepository.save(settings)
  }
}
