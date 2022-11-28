package io.tolgee.dtos.cacheable

import io.tolgee.model.enums.Scope
import java.io.Serializable

data class PermissionDto(
  val id: Long,
  val userId: Long?,
  val invitationId: Long?,
  val scopes: Array<Scope>,
  val projectId: Long?,
  val organizationId: Long?,
  val languageIds: MutableSet<Long>? = null
) : Serializable
