package io.tolgee.service

import io.tolgee.constants.ApiScope
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.repository.ApiKeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

@Service
open class ApiKeyService @Autowired constructor(private val apiKeyRepository: ApiKeyRepository,
                                                private val random: SecureRandom) {

    @set:Autowired
    lateinit var permissionService: PermissionService

    open fun createApiKey(userAccount: UserAccount?, scopes: Set<ApiScope>, project: Project?): ApiKeyDTO {
        val apiKey = ApiKey(
                key = BigInteger(130, random).toString(32),
                project = project,
                userAccount = userAccount,
                scopesEnum = scopes
        )
        apiKeyRepository.save(apiKey)
        return ApiKeyDTO.fromEntity(apiKey)
    }

    open fun getAllByUser(userAccount: UserAccount?): Set<ApiKey> {
        return apiKeyRepository.getAllByUserAccountOrderById(userAccount)
    }

    open fun getAllByRepository(repositoryId: Long?): Set<ApiKey> {
        return apiKeyRepository.getAllByProjectId(repositoryId)
    }

    open fun getApiKey(apiKey: String?): Optional<ApiKey> {
        return apiKeyRepository.findByKey(apiKey)
    }

    open fun getApiKey(id: Long): Optional<ApiKey> {
        return apiKeyRepository.findById(id)
    }

    open fun deleteApiKey(apiKey: ApiKey) {
        apiKeyRepository.delete(apiKey)
    }

    open fun getAvailableScopes(userAccount: UserAccount, project: Project): Set<ApiScope> {
        return permissionService.getRepositoryPermissionType(project.id, userAccount)?.availableScopes?.toSet()
                ?: throw NotFoundException()
    }

    open fun editApiKey(apiKey: ApiKey) {
        apiKeyRepository.save(apiKey)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        apiKeyRepository.deleteAllByProjectId(repositoryId)
    }
}
