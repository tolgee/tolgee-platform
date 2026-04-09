package io.tolgee.service.project

import io.tolgee.constants.Feature
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.model.Project

object ProjectFeatureRegistry {
  private val projectFeatureChecks: Map<Feature, (Project) -> Boolean> =
    mapOf(
      Feature.BRANCHING to { it.useBranching },
      Feature.QA_CHECKS to { it.useQaChecks },
    )

  private val projectDtoFeatureChecks: Map<Feature, (ProjectDto) -> Boolean> =
    mapOf(
      Feature.BRANCHING to { it.useBranching },
      Feature.QA_CHECKS to { it.useQaChecks },
    )

  fun isEnabledOnProject(
    feature: Feature,
    project: Project,
  ): Boolean {
    return projectFeatureChecks[feature]?.invoke(project) ?: true
  }

  fun isEnabledOnProject(
    feature: Feature,
    project: ProjectDto,
  ): Boolean {
    return projectDtoFeatureChecks[feature]?.invoke(project) ?: true
  }
}
