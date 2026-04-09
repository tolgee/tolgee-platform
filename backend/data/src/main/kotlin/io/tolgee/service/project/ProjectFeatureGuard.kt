package io.tolgee.service.project

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Project
import io.tolgee.security.ProjectHolder
import org.springframework.stereotype.Service

@Service
class ProjectFeatureGuard(
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val projectHolder: ProjectHolder,
) {
  fun checkIfUsed(
    feature: Feature,
    value: Any?,
  ) {
    if (!isValueUsed(value)) return
    checkEnabled(feature)
  }

  fun checkEnabled(feature: Feature) {
    val project = projectHolder.projectEntity
    enabledFeaturesProvider.checkFeatureEnabled(project.organizationOwner.id, feature)
    if (!ProjectFeatureRegistry.isEnabledOnProject(feature, project)) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED_FOR_PROJECT, listOf(feature.name))
    }
  }

  fun checkEnabled(
    feature: Feature,
    project: Project,
  ) {
    enabledFeaturesProvider.checkFeatureEnabled(project.organizationOwner.id, feature)
    if (!ProjectFeatureRegistry.isEnabledOnProject(feature, project)) {
      throw BadRequestException(Message.FEATURE_NOT_ENABLED_FOR_PROJECT, listOf(feature.name))
    }
  }

  fun isFeatureEnabled(
    feature: Feature,
    project: Project = projectHolder.projectEntity,
  ): Boolean {
    if (!enabledFeaturesProvider.isFeatureEnabled(project.organizationOwner.id, feature)) {
      return false
    }
    if (!ProjectFeatureRegistry.isEnabledOnProject(feature, project)) {
      return false
    }
    return true
  }

  fun isFeatureEnabled(
    feature: Feature,
    project: ProjectDto = projectHolder.project,
  ): Boolean {
    if (!enabledFeaturesProvider.isFeatureEnabled(project.organizationOwnerId, feature)) {
      return false
    }
    if (!ProjectFeatureRegistry.isEnabledOnProject(feature, project)) {
      return false
    }
    return true
  }

  private fun isValueUsed(value: Any?): Boolean {
    return when (value) {
      null -> false
      is String -> value.isNotBlank()
      is Collection<*> -> value.isNotEmpty()
      is Array<*> -> value.isNotEmpty()
      is Boolean -> value
      else -> true
    }
  }
}
