package io.tolgee.service

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.exceptions.InvalidStateException
import io.tolgee.exceptions.NotFoundException
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

  @set:Autowired
  lateinit var userAccountService: UserAccountService

  @Transactional
  fun grantFullAccessToRepo(project: Project) {
    permissionService.grantFullAccessToProject(
      userAccountService[activeUser.id]
        .orElseThrow { InvalidStateException() }!!,
      project
    )
  }

  fun checkAnyProjectPermission(projectId: Long): ProjectPermissionType {
    return getProjectPermission(projectId) ?: throw PermissionException()
  }

  fun checkAnyProjectPermission(projectId: Long, userId: Long): ProjectPermissionType {
    return getProjectPermission(projectId, userId) ?: throw PermissionException()
  }

  fun checkProjectPermission(projectId: Long, requiredPermission: ProjectPermissionType): ProjectPermissionType {
    val usersPermission = checkAnyProjectPermission(projectId)
    if (requiredPermission.power > usersPermission.power) {
      throw PermissionException()
    }
    return usersPermission
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

  private fun getProjectPermission(projectId: Long, userId: Long = activeUser.id): ProjectPermissionType? {
    return permissionService.getProjectPermissionType(projectId, userId)
  }

  private val activeUser: UserAccountDto
    get() = authenticationFacade.userAccount
}
