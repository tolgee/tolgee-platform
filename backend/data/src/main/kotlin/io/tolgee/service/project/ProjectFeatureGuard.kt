package io.tolgee.service.project

import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.dtos.request.validators.exceptions.ValidationException
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
      throw ValidationException(Message.FEATURE_NOT_ENABLED)
    }
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
