package io.tolgee.dtos.request.project

import io.tolgee.model.Permission.ProjectPermissionType
import javax.validation.constraints.NotNull

data class ProjectInviteUserDto(
  @field:NotNull
  var type: ProjectPermissionType? = null
)
