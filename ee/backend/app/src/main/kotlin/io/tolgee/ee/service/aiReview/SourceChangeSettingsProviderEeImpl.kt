package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Caches
import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.service.aiReview.EffectiveSourceChangeSettings
import io.tolgee.service.aiReview.SourceChangeSettingsProvider
import io.tolgee.service.project.ProjectFeatureGuard
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class SourceChangeSettingsProviderEeImpl(
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val entityManager: EntityManager,
) : SourceChangeSettingsProvider {
  override fun getEffective(project: ProjectDto): EffectiveSourceChangeSettings {
    val protect = isProtectReviewed(project)
    val aiApprovedApplies = !protect && projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)
    return EffectiveSourceChangeSettings(
      aiApproved =
        when {
          aiApprovedApplies -> project.aiApprovedSourceChangeAction
          else -> EffectiveSourceChangeSettings.DEFAULTS.aiApproved
        },
      humanEdited = project.humanEditedSourceChangeAction,
      humanReviewed =
        when {
          protect -> SourceChangeAction.MARK_OUTDATED
          else -> project.humanReviewedSourceChangeAction
        },
    )
  }

  fun isProtectReviewed(project: ProjectDto): Boolean =
    project.translationProtection == TranslationProtection.PROTECT_REVIEWED

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  fun update(
    projectId: Long,
    aiApproved: SourceChangeAction,
    humanEdited: SourceChangeAction,
    humanReviewed: SourceChangeAction,
  ): Project {
    val project = entityManager.find(Project::class.java, projectId)
    val protect = project.translationProtection == TranslationProtection.PROTECT_REVIEWED
    if (!protect && projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) {
      project.aiApprovedSourceChangeAction = aiApproved
    }
    project.humanEditedSourceChangeAction = humanEdited
    if (!protect) {
      project.humanReviewedSourceChangeAction = humanReviewed
    }
    return project
  }
}
