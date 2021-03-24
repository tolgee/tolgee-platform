package io.tolgee.service

import io.tolgee.constants.ApiScope
import io.tolgee.dtos.response.ApiKeyDTO.ApiKeyDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
import io.tolgee.repository.ApiKeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*
import java.util.stream.Collectors

@Service
open class ApiKeyService @Autowired constructor(private val apiKeyRepository: ApiKeyRepository, private val permissionService: PermissionService, private val random: SecureRandom) {
    open fun createApiKey(userAccount: UserAccount?, scopes: Set<ApiScope>, repository: Repository?): ApiKeyDTO {
        val apiKey = ApiKey(
                key = BigInteger(130, random).toString(32),
                repository = repository,
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
        return apiKeyRepository.getAllByRepositoryId(repositoryId)
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

    open fun getAvailableScopes(userAccount: UserAccount?, repository: Repository): Set<ApiScope> {
        return Arrays.stream(
                permissionService.getRepositoryPermission(repository.id, userAccount)
                        .orElseThrow { NotFoundException() }.type!!.availableScopes
        ).collect(Collectors.toSet())
    }

    open fun editApiKey(apiKey: ApiKey) {
        apiKeyRepository.save(apiKey)
    }

    open fun deleteAllByRepository(repositoryId: Long?) {
        apiKeyRepository.deleteAllByRepositoryId(repositoryId)
    }
}
