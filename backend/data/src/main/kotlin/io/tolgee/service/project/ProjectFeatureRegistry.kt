package io.tolgee.service.project

import io.tolgee.constants.Feature
import io.tolgee.model.Project

object ProjectFeatureRegistry {
  private val projectFeatureChecks: Map<Feature, (Project) -> Boolean> =
    mapOf(
      Feature.BRANCHING to { it.useBranching },
    )

  fun isEnabledOnProject(
    feature: Feature,
    project: Project,
  ): Boolean {
    return projectFeatureChecks[feature]?.invoke(project) ?: true
  }
}
