package io.tolgee.dtos

import io.tolgee.constants.ComputedPermissionOrigin
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class ComputedPermissionDto(
  permission: IPermission,
  val origin: ComputedPermissionOrigin = ComputedPermissionOrigin.NONE
) : IPermission by permission {
  val allTranslateLanguagesPermitted: Boolean
    get() {
      if (scopes.isNullOrEmpty()) {
        return false
      }

      if (translateLanguageIds.isNullOrEmpty()) {
        return true
      }

      if (scopes.contains(Scope.ADMIN)) {
        return true
      }

      if (scopes.contains(Scope.TRANSLATIONS_EDIT)) {
        return translateLanguageIds.isEmpty()
      }

      return false
    }

  constructor(permission: IPermission) : this(
    permission,
    origin = if (permission.organizationId != null)
      ComputedPermissionOrigin.ORGANIZATION_BASE
    else
      ComputedPermissionOrigin.DIRECT
  )

  companion object {
    private fun getEmptyPermission(scopes: Array<Scope>, type: ProjectPermissionType): IPermission {
      return object : IPermission {
        override val scopes: Array<Scope>
          get() = scopes
        override val projectId: Long?
          get() = null
        override val organizationId: Long?
          get() = null
        override val translateLanguageIds: Set<Long>?
          get() = null
        override val viewLanguageIds: Set<Long>?
          get() = null
        override val stateChangeLanguageIds: Set<Long>?
          get() = null
        override val type: ProjectPermissionType
          get() = type
        override val granular: Boolean?
          get() = null
      }
    }

    val NONE
      get() = ComputedPermissionDto(getEmptyPermission(scopes = arrayOf(), ProjectPermissionType.NONE))
    val ADMIN
      get() = ComputedPermissionDto(
        getEmptyPermission(
          scopes = arrayOf(Scope.ADMIN),
          type = ProjectPermissionType.MANAGE
        ),
        origin = ComputedPermissionOrigin.ADMIN
      )
  }
}
