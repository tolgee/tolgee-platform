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

  val isAllPermitted = this.expandedScopes.toSet().containsAll(Scope.entries)

  /**
   * Admin and Supporter users have some additional permissions on all projects compared to other users.
   * This function adds all the additional permissions the user has the right to use based on their role.
   */
  fun getAdminOrSupporterPermissions(userRole: UserAccount.Role?): ComputedPermissionDto {
    if (userRole == UserAccount.Role.ADMIN && !this.isAllPermitted) {
      return SERVER_ADMIN
    }
    if (userRole == UserAccount.Role.SUPPORTER && !this.isAllReadOnlyPermitted) {
      if (this.type == ProjectPermissionType.NONE && this.scopes.isEmpty()) {
        // optimization - if a user doesn't have any permissions,
        // we can return static override the same as we do for admin,
        // otherwise we have to calculate permissions specific for them
        return SERVER_SUPPORTER
      }
      return ComputedPermissionDto(
        getExtendedPermission(this, arrayOf(Scope.ALL_VIEW)),
        origin = ComputedPermissionOrigin.SERVER_SUPPORTER,
      )
    }
    return this
  }

  fun withCommunityFloor(): ComputedPermissionDto {
    val hasCommunityScopes = expandedScopes.toSet().containsAll(COMMUNITY.scopes.toList())
    val viewAndSuggestUnrestricted = viewLanguageIds.isNullOrEmpty() && suggestLanguageIds.isNullOrEmpty()
    if (hasCommunityScopes && viewAndSuggestUnrestricted) return this
    if (scopes.isEmpty()) return COMMUNITY
    return ComputedPermissionDto(getCommunityFlooredPermission(this), origin = origin)
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

    private fun getExtendedPermission(
      base: IPermission,
      extendedScopes: Array<Scope>,
    ): IPermission {
      return object : IPermission by base {
        override val scopes: Array<Scope> by lazy {
          (base.scopes + extendedScopes).toSet().toTypedArray()
        }
      }
    }

    private fun getCommunityFlooredPermission(base: IPermission): IPermission {
      return object : IPermission by getExtendedPermission(base, COMMUNITY.scopes) {
        // All-language view + suggest: a language-restricted base must not leave a member
        // viewing/suggesting in fewer languages than a non-member.
        override val viewLanguageIds: Set<Long>?
          get() = null
        override val suggestLanguageIds: Set<Long>?
          get() = null
      }
    }

    val ComputedPermissionDto.isAllReadOnlyPermitted: Boolean
      get() = expandedScopes.toSet().containsAll(Scope.readOnlyScopes.toList())

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
    val SERVER_SUPPORTER
      get() =
        ComputedPermissionDto(
          getEmptyPermission(
            scopes = arrayOf(Scope.ALL_VIEW),
            type = ProjectPermissionType.VIEW,
          ),
          origin = ComputedPermissionOrigin.SERVER_SUPPORTER,
        )

    val COMMUNITY
      get() =
        ComputedPermissionDto(
          getEmptyPermission(
            scopes =
              arrayOf(
                Scope.TRANSLATIONS_VIEW,
                Scope.SCREENSHOTS_VIEW,
                Scope.ACTIVITY_VIEW,
                Scope.TRANSLATIONS_SUGGEST,
                Scope.TRANSLATIONS_COMMENTS_ADD,
              ),
            type = ProjectPermissionType.VIEW,
          ),
          origin = ComputedPermissionOrigin.COMMUNITY,
        )
  }
}
