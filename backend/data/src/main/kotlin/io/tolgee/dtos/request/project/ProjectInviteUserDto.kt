package io.tolgee.dtos.request.project

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.Permission.ProjectPermissionType
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class ProjectInviteUserDto(
  @field:NotNull
  var type: ProjectPermissionType? = null,

  @Schema(
    description = """IDs of languages to allow user to translate to with TRANSLATE permission.

Only applicable when type is TRANSLATE, otherwise 400 - Bad Request is returned.
  """
  )
  var languages: Set<Long>? = null,

  @Schema(
    description = """Email to send invitation to"""
  )
  @field:Size(max = 250)
  @field:Email
  val email: String? = null,

  @Schema(
    description = """Name of invited user"""
  )
  @field:Size(max = 250)
  val name: String? = null
)
