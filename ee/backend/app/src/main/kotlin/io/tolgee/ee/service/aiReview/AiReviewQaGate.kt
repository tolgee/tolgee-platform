package io.tolgee.ee.service.aiReview

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.ee.service.qa.ProjectQaConfigService
import io.tolgee.model.Project
import io.tolgee.model.enums.qa.QaIssueState
import io.tolgee.repository.qa.TranslationQaIssueRepository
import io.tolgee.service.project.ProjectFeatureGuard
import org.springframework.stereotype.Service

@Service
class AiReviewQaGate(
  private val projectFeatureGuard: ProjectFeatureGuard,
  private val projectQaConfigService: ProjectQaConfigService,
  private val qaIssueRepository: TranslationQaIssueRepository,
) {
  fun isQaOn(project: ProjectDto): Boolean =
    project.useQaChecks && projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)

  fun qaLanguageIds(project: ProjectDto): Set<Long> {
    if (!isQaOn(project)) return emptySet()
    return projectQaConfigService.getEnabledLanguageIds(project.id)
  }

  fun isQaOnForLanguage(
    project: Project,
    languageId: Long,
  ): Boolean {
    if (!project.useQaChecks) return false
    if (!projectFeatureGuard.isFeatureEnabled(Feature.QA_CHECKS, project)) return false
    return languageId in projectQaConfigService.getEnabledLanguageIds(project.id)
  }

  fun hasOpenQaIssues(translationId: Long): Boolean =
    qaIssueRepository.existsByTranslationIdAndState(translationId, QaIssueState.OPEN)

  fun translationIdsWithOpenIssues(translationIds: Collection<Long>): Set<Long> {
    if (translationIds.isEmpty()) return emptySet()
    return qaIssueRepository.findTranslationIdsByState(translationIds, QaIssueState.OPEN).toSet()
  }
}
