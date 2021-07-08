package io.tolgee.dtos.request

import io.tolgee.model.enums.OrganizationRoleType
import javax.validation.constraints.NotNull

data class OrganizationInviteUserDto(
  @field:NotNull
  var roleType: OrganizationRoleType = OrganizationRoleType.MEMBER
)
