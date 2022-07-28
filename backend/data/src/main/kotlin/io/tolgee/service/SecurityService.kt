package io.tolgee.service

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.model.translation.Translation
import io.tolgee.security.AuthenticationFacade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SecurityService @Autowired constructor(
  private val authenticationFacade: AuthenticationFacade,
  private val languageService: LanguageService
) {

  @set:Autowired
  lateinit var apiKeyService: ApiKeyService

  @set:Autowired
  lateinit var permissionService: PermissionService

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  fun checkAnyProjectPermission(projectId: Long) {
    if (getProjectPermission(projectId) == null && !isCurrentUserServerAdmin()) throw PermissionException()
  }

  fun checkAnyProjectPermission(projectId: Long, userId: Long): ProjectPermissionType {
    return getProjectPermission(projectId, userId) ?: throw PermissionException()
  }

  fun checkProjectPermission(projectId: Long, requiredPermission: ProjectPermissionType) {
    if (isCurrentUserServerAdmin()) {
      return
    }
    val usersPermission = getProjectPermission(projectId) ?: throw PermissionException()
    if (requiredPermission.power > usersPermission.power || !isCurrentUserServerAdmin()) {
      throw PermissionException()
    }
  }

  fun checkLanguageTranslatePermission(projectId: Long, languageIds: Collection<Long>) {
    if (isCurrentUserServerAdmin()) {
      return
    }
    val usersPermission = permissionService.getProjectPermissionData(projectId, authenticationFacade.userAccount.id)
    val permittedLanguages = usersPermission.computedPermissions.languageIds
    if (usersPermission.computedPermissions.allLanguagesPermitted) {
      return
    }
    if (permittedLanguages?.containsAll(languageIds) != true) {
      throw PermissionException()
    }
  }

  fun checkLanguageTranslatePermission(translation: Translation) {
    val language = translation.language
    checkLanguageTranslatePermission(language.project.id, listOf(language.id))
  }

  fun checkLanguageTagPermissions(tags: Set<String>, projectId: Long) {
    val languages = languageService.findByTags(tags, projectId)
    this.checkLanguageTranslatePermission(projectId, languages.map { it.id })
  }

  fun checkApiKeyScopes(scopes: Set<ApiScope>, project: Project?, user: UserAccount? = null) {
    try {
      if (!apiKeyService.getAvailableScopes(user?.id ?: activeUser.id, project!!).containsAll(scopes)) {
        throw PermissionException()
      }
    } catch (e: NotFoundException) {
      throw PermissionException()
    }
  }

  fun checkApiKeyScopes(scopes: Set<ApiScope>, apiKey: ApiKey) {
    // checks if user's has permissions to use api key with api key's permissions
    checkApiKeyScopes(scopes, apiKey.project, apiKey.userAccount)
    if (!apiKey.scopesEnum.containsAll(scopes)) {
      throw PermissionException()
    }
  }

  fun checkScreenshotsUploadPermission(projectId: Long) {
    if (authenticationFacade.isApiKeyAuthentication) {
      checkApiKeyScopes(setOf(ApiScope.SCREENSHOTS_UPLOAD), authenticationFacade.apiKey)
    }
    checkProjectPermission(projectId, ProjectPermissionType.TRANSLATE)
  }

  fun checkUserIsServerAdmin() {
    if (authenticationFacade.userAccount.role != UserAccount.Role.ADMIN) {
      throw PermissionException()
    }
  }

  private fun getProjectPermission(projectId: Long, userId: Long = activeUser.id): ProjectPermissionType? {
    return permissionService.getProjectPermissionType(projectId, userId)
  }

  private fun isCurrentUserServerAdmin(): Boolean {
    return activeUser.role == UserAccount.Role.ADMIN
  }

  private val activeUser: UserAccountDto
    get() = authenticationFacade.userAccount
}
