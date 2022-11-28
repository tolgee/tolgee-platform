package io.tolgee.dtos.cacheable

import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

interface IPermission {
  val scopes: Array<Scope>
  val projectId: Long?
  val organizationId: Long?
  val translateLanguageIds: Set<Long>?
  val viewLanguageIds: Set<Long>?
  val stateChangeLanguageIds: Set<Long>?
  val type: ProjectPermissionType?
  val granular: Boolean?
}
