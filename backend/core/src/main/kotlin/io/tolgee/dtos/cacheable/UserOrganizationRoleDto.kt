package io.tolgee.dtos.cacheable

import io.tolgee.model.OrganizationRole
import io.tolgee.model.enums.OrganizationRoleType

data class UserOrganizationRoleDto(
  val userId: Long,
  val type: OrganizationRoleType?,
) {
  companion object {
    fun fromEntity(
      userId: Long,
      entity: OrganizationRole?,
    ) = UserOrganizationRoleDto(
      userId = userId,
      type = entity?.type,
    )
  }
}
