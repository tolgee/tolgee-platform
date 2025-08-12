package io.tolgee.dtos

import io.tolgee.constants.ComputedPermissionOrigin
import io.tolgee.dtos.cacheable.IPermission
import io.tolgee.exceptions.LanguageNotPermittedException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.Scope

class ComputedPermissionDto(
  permission: IPermission,
  val origin: ComputedPermissionOrigin = ComputedPermissionOrigin.NONE,
) : IPermission by permission {
  val expandedScopes: Array<Scope> by lazy {
    Scope.expand(this.scopes)
  }

  fun checkViewPermitted(vararg languageIds: Long) = checkLanguagePermitted(languageIds.toList(), viewLanguageIds)

  fun checkTranslatePermitted(vararg languageIds: Long) =
    checkLanguagePermitted(
      languageIds.toList(),
      translateLanguageIds,
    )

  fun checkStateChangePermitted(vararg languageIds: Long) =
    checkLanguagePermitted(
      languageIds.toList(),
      stateChangeLanguageIds,
    )

  fun checkSuggestPermitted(vararg languageIds: Long) =
    checkLanguagePermitted(
      languageIds.toList(),
      suggestLanguageIds,
    )

  private fun isAllLanguagesPermitted(languageIds: Collection<Long>?): Boolean {
    if (scopes.isEmpty()) {
      return false
    }

    if (languageIds.isNullOrEmpty()) {
      return true
    }

    if (scopes.contains(Scope.ADMIN)) {
      return true
    }

    return false
  }

  private fun checkLanguagePermitted(
    languageIds: Collection<Long>,
    permittedLanguageIds: Collection<Long>?,
  ) {
    if (this.isAllLanguagesPermitted(permittedLanguageIds)) {
      return
    }
    if (permittedLanguageIds?.containsAll(languageIds) != true) {
      throw LanguageNotPermittedException(languageIds = languageIds - permittedLanguageIds.orEmpty().toSet())
    }
  }

  val isAllPermitted = this.expandedScopes.toSet().containsAll(Scope.values().toList())

  fun getAdminPermissions(userRole: UserAccount.Role?): ComputedPermissionDto {
    if (userRole == UserAccount.Role.ADMIN && !this.isAllPermitted) {
      return SERVER_ADMIN
    }
    return this
  }

  constructor(permission: IPermission) : this(
    permission,
    origin =
      if (permission.organizationId != null) {
        ComputedPermissionOrigin.ORGANIZATION_BASE
      } else {
        ComputedPermissionOrigin.DIRECT
      },
  )

  companion object {
    private fun getEmptyPermission(
      scopes: Array<Scope>,
      type: ProjectPermissionType,
    ): IPermission {
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
        override val suggestLanguageIds: Set<Long>?
          get() = null
        override val type: ProjectPermissionType
          get() = type
        override val granular: Boolean?
          get() = null
      }
    }

    val NONE
      get() = ComputedPermissionDto(getEmptyPermission(scopes = arrayOf(), ProjectPermissionType.NONE))
    val ORGANIZATION_OWNER
      get() =
        ComputedPermissionDto(
          getEmptyPermission(
            scopes = arrayOf(Scope.ADMIN),
            type = ProjectPermissionType.MANAGE,
          ),
          origin = ComputedPermissionOrigin.ORGANIZATION_OWNER,
        )
    val SERVER_ADMIN
      get() =
        ComputedPermissionDto(
          getEmptyPermission(
            scopes = arrayOf(Scope.ADMIN),
            type = ProjectPermissionType.MANAGE,
          ),
          origin = ComputedPermissionOrigin.SERVER_ADMIN,
        )
  }
}
