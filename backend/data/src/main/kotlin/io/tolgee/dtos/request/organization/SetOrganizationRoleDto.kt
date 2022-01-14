package io.tolgee.dtos.request.organization

import io.tolgee.model.enums.OrganizationRoleType
import javax.validation.constraints.NotBlank

data class SetOrganizationRoleDto(
  @NotBlank
  val roleType: OrganizationRoleType = OrganizationRoleType.MEMBER
)
