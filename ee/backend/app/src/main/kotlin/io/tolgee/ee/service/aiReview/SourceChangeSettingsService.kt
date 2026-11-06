package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project
import io.tolgee.model.enums.SourceChangeAction
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.service.aiReview.EffectiveSourceChangeSettings
import io.tolgee.service.aiReview.SourceChangeSettingsProvider
import io.tolgee.service.project.ProjectFeatureGuard
import io.tolgee.service.project.ProjectService
import jakarta.persistence.EntityManager
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Primary
@Service
class SourceChangeSettingsService(
  private val projectService: ProjectService,
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val entityManager: EntityManager,
) : SourceChangeSettingsProvider {
  override fun getEffective(project: ProjectDto): EffectiveSourceChangeSettings {
    val protect = project.translationProtection == TranslationProtection.PROTECT_REVIEWED
    val aiApprovedApplies = !protect && projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)
    return EffectiveSourceChangeSettings(
      aiApproved =
        if (aiApprovedApplies) {
          project.aiApprovedSourceChangeAction
        } else {
          EffectiveSourceChangeSettings.DEFAULTS.aiApproved
        },
      humanEdited = project.humanEditedSourceChangeAction,
      humanReviewed = if (protect) SourceChangeAction.MARK_OUTDATED else project.humanReviewedSourceChangeAction,
    )
  }

  fun isProtectReviewed(projectId: Long): Boolean =
    projectService.getDto(projectId).translationProtection == TranslationProtection.PROTECT_REVIEWED

  @Transactional
  fun update(
    projectId: Long,
    aiApproved: SourceChangeAction,
    humanEdited: SourceChangeAction,
    humanReviewed: SourceChangeAction,
  ): Project {
    val project = entityManager.find(Project::class.java, projectId)
    if (projectFeatureGuard.isFeatureEnabled(Feature.AI_REVIEW, project)) {
      project.aiApprovedSourceChangeAction = aiApproved
    }
    project.humanEditedSourceChangeAction = humanEdited
    project.humanReviewedSourceChangeAction = humanReviewed
    return project
  }
}
