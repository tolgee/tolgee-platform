package io.tolgee.dtos.cacheable

import io.tolgee.model.OrganizationRole
import io.tolgee.model.enums.OrganizationRoleType

data class OrganizationRoleDto(
  val userId: Long?,
  val type: OrganizationRoleType,
) {
  companion object {
    fun fromEntity(entity: OrganizationRole) =
      OrganizationRoleDto(
        userId = entity.user?.id,
        type = entity.type,
      )
  }
}
