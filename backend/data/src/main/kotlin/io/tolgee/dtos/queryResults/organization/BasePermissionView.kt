package io.tolgee.dtos.queryResults.organization

import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class BasePermissionView(
  private val _scopes: Array<Scope>?,
  override val type: ProjectPermissionType?,
) : IPermission {
  override val scopes: Array<Scope>
    get() = getScopesFromTypeAndScopes(type, _scopes)

  override val projectId: Long? = null
  override val organizationId: Long = 0
  override val translateLanguageIds: Set<Long>? = null
  override val viewLanguageIds: Set<Long>? = null
  override val stateChangeLanguageIds: Set<Long>? = null
  override val suggestLanguageIds: Set<Long>? = null
  override val granular: Boolean
    get() = _scopes != null

  companion object {
    fun of(entity: IPermission): BasePermissionView {
      return BasePermissionView(
        _scopes = entity.scopes,
        type = entity.type,
      )
    }
  }
}
