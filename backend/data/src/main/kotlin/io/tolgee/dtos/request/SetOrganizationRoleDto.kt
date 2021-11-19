package io.tolgee.dtos.request

import io.tolgee.model.enums.OrganizationRoleType
import javax.validation.constraints.NotBlank

data class SetOrganizationRoleDto(
  @NotBlank
  val roleType: OrganizationRoleType = OrganizationRoleType.MEMBER
)
