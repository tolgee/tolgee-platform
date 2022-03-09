package io.tolgee.dtos.cacheable

import io.tolgee.model.Permission
import java.io.Serializable

data class PermissionDto(
  val id: Long,
  val userId: Long?,
  val invitationId: Long?,
  val type: Permission.ProjectPermissionType,
  val projectId: Long,
  val languageIds: MutableSet<Long>? = null
) : Serializable
