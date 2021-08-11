package io.tolgee.dtos.cacheable

import io.tolgee.model.Permission
import java.io.Serializable

data class PermissionDto(
  val id: Long,
  val userId: Long?,
  val invitationId: Long?,
  val type: Permission.ProjectPermissionType,
  val projectId: Long
) : Serializable {
  companion object {
    fun fromEntity(entity: Permission) = PermissionDto(
      userId = entity.user?.id,
      invitationId = entity.invitation?.id,
      id = entity.id,
      type = entity.type,
      projectId = entity.project.id
    )
  }
}
