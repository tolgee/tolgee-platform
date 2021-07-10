package io.tolgee.service

import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.ProjectPermissionType
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.security.AuthenticationFacade
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SecurityService @Autowired constructor(private val authenticationFacade: AuthenticationFacade) {

  @set:Autowired
  lateinit var apiKeyService: ApiKeyService

  @set:Autowired
  lateinit var permissionService: PermissionService

  @Transactional
  fun grantFullAccessToRepo(project: Project?) {
    permissionService.grantFullAccessToRepo(activeUser, project)
  }

  fun checkAnyProjectPermission(projectId: Long): ProjectPermissionType {
    return getProjectPermission(projectId) ?: throw PermissionException()
  }

  fun checkAnyProjectPermission(projectId: Long, user: UserAccount): ProjectPermissionType {
    return getProjectPermission(projectId, user) ?: throw PermissionException()
  }

  fun checkProjectPermission(projectId: Long, requiredPermission: ProjectPermissionType): ProjectPermissionType {
    val usersPermission = checkAnyProjectPermission(projectId)
    if (requiredPermission.power > usersPermission.power) {
      throw PermissionException()
    }
    return usersPermission
  }

  fun checkApiKeyScopes(scopes: Set<ApiScope>, project: Project?, user: UserAccount? = null) {
    if (!apiKeyService.getAvailableScopes(user ?: activeUser, project!!).containsAll(scopes)) {
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

  private fun getProjectPermission(projectId: Long, user: UserAccount = activeUser): ProjectPermissionType? {
    return permissionService.getProjectPermissionType(projectId, user)
  }

  private val activeUser: UserAccount
    get() = authenticationFacade.userAccount
}
