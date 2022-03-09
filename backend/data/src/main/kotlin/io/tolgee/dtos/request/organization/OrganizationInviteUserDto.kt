package io.tolgee.dtos.request.organization

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.OrganizationRoleType
import javax.validation.constraints.Email
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

data class OrganizationInviteUserDto(
  @field:NotNull
  var roleType: OrganizationRoleType = OrganizationRoleType.MEMBER,

  @Schema(
    description = """Name of invited user"""
  )
  @field:Size(max = 250)
  var name: String? = null,

  @Schema(
    description = """Email to send invitation to"""
  )
  @field:Size(max = 250)
  @field:Email
  var email: String? = null
)
