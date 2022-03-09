package io.tolgee.dtos

import io.tolgee.model.Permission

data class ComputedPermissionDto(
  val type: Permission.ProjectPermissionType?,
  val languageIds: Set<Long>?
) {
  val allLanguagesPermitted: Boolean
    get() = type != Permission.ProjectPermissionType.TRANSLATE || languageIds.isNullOrEmpty()
}
