package io.tolgee.service.security

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.model.translation.Translation
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.LanguageService
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
    if (getProjectPermissionScopes(projectId) == null && !isCurrentUserServerAdmin()) throw PermissionException()
  }

  fun checkProjectPermission(projectId: Long, requiredPermission: Scope, userAccountDto: UserAccountDto) {
    if (isUserAdmin(userAccountDto)) {
      return
    }

    val usersPermissionScopes = getProjectPermissionScopes(projectId, userAccountDto.id)
      ?: throw PermissionException()

    if (!usersPermissionScopes.contains(requiredPermission)) {
      throw PermissionException()
    }
  }

  fun checkProjectPermission(projectId: Long, requiredPermission: Scope) {
    checkProjectPermission(projectId, requiredPermission, activeUser)
  }

  fun checkLanguageTranslatePermission(projectId: Long, languageIds: Collection<Long>) {
    if (isCurrentUserServerAdmin()) {
      return
    }
    val usersPermission = permissionService.getProjectPermissionData(projectId, authenticationFacade.userAccount.id)
    val permittedLanguages = usersPermission.computedPermissions.translateLanguageIds
    if (usersPermission.computedPermissions.allTranslateLanguagesPermitted) {
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

  fun checkApiKeyScopes(scopes: Set<Scope>, project: Project?, user: UserAccount? = null) {
    try {
      val availableScopes = apiKeyService.getAvailableScopes(user?.id ?: activeUser.id, project!!)
      val userCanSelectTheScopes = availableScopes.toList().containsAll(scopes)
      if (!userCanSelectTheScopes) {
        throw PermissionException()
      }
    } catch (e: NotFoundException) {
      throw PermissionException()
    }
  }

  fun checkApiKeyScopes(scopes: Set<Scope>, apiKey: ApiKey) {
    // checks if user's has permissions to use api key with api key's permissions
    checkApiKeyScopes(scopes, apiKey.project, apiKey.userAccount)
    if (!apiKey.scopesEnum.containsAll(scopes)) {
      throw PermissionException()
    }
  }

  fun checkScreenshotsUploadPermission(projectId: Long) {
    if (authenticationFacade.isApiKeyAuthentication) {
      checkApiKeyScopes(setOf(Scope.SCREENSHOTS_UPLOAD), authenticationFacade.apiKey)
    }
    checkProjectPermission(projectId, Scope.SCREENSHOTS_UPLOAD)
  }

  fun checkUserIsServerAdmin() {
    if (authenticationFacade.userAccount.role != UserAccount.Role.ADMIN) {
      throw PermissionException()
    }
  }

  fun getProjectPermissionScopes(projectId: Long, userId: Long = activeUser.id): Array<Scope>? {
    return permissionService.getProjectPermissionScopes(projectId, userId)
  }

  private fun isCurrentUserServerAdmin(): Boolean {
    return isUserAdmin(activeUser)
  }

  private fun isUserAdmin(user: UserAccountDto): Boolean {
    return user.role == UserAccount.Role.ADMIN
  }

  private val activeUser: UserAccountDto
    get() = authenticationFacade.userAccount
}
