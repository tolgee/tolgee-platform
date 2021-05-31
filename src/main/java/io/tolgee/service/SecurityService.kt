package io.tolgee.service

import io.tolgee.constants.ApiScope
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.ApiKey
import io.tolgee.model.Permission.RepositoryPermissionType
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
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
    fun grantFullAccessToRepo(repository: Repository?) {
        permissionService.grantFullAccessToRepo(activeUser, repository)
    }

    fun checkAnyRepositoryPermission(repositoryId: Long): RepositoryPermissionType {
        return getRepositoryPermission(repositoryId) ?: throw PermissionException()
    }

    fun checkRepositoryPermission(repositoryId: Long, requiredPermission: RepositoryPermissionType): RepositoryPermissionType {
        val usersPermission = checkAnyRepositoryPermission(repositoryId)
        if (requiredPermission.power > usersPermission.power) {
            throw PermissionException()
        }
        return usersPermission
    }

    fun checkApiKeyScopes(scopes: Set<ApiScope>, repository: Repository?) {
        if (!apiKeyService.getAvailableScopes(activeUser, repository!!).containsAll(scopes)) {
            throw PermissionException()
        }
    }

    fun checkApiKeyScopes(scopes: Set<ApiScope>, apiKey: ApiKey) {
        checkApiKeyScopes(scopes, apiKey.repository) // checks if user's has permissions to use api key with api key's permissions
        if (!apiKey.scopesEnum.containsAll(scopes)) {
            throw PermissionException()
        }
    }

    private fun getRepositoryPermission(repositoryId: Long): RepositoryPermissionType? {
        return permissionService.getRepositoryPermissionType(repositoryId, activeUser)
    }

    private val activeUser: UserAccount
        get() = authenticationFacade.userAccount
}
