package io.tolgee.service

import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.ApiKey
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ApiScope
import io.tolgee.repository.ApiKeyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.security.SecureRandom
import java.util.*

@Service
class ApiKeyService @Autowired constructor(
  private val apiKeyRepository: ApiKeyRepository,
  private val random: SecureRandom
) {

  @set:Autowired
  lateinit var permissionService: PermissionService

  fun create(userAccount: UserAccount, scopes: Set<ApiScope>, project: Project): ApiKey {
    val apiKey = ApiKey(
      key = BigInteger(130, random).toString(32),
      project = project,
      userAccount = userAccount,
      scopesEnum = scopes
    )
    return apiKeyRepository.save(apiKey)
  }

  fun getAllByUser(userAccountId: Long): Set<ApiKey> {
    return apiKeyRepository.getAllByUserAccountIdOrderById(userAccountId)
  }

  fun getAllByUser(userAccountId: Long, filterProjectId: Long?, pageable: Pageable): Page<ApiKey> {
    return apiKeyRepository.getAllByUserAccount(userAccountId, filterProjectId, pageable)
  }

  fun getAllByProject(projectId: Long): Set<ApiKey> {
    return apiKeyRepository.getAllByProjectId(projectId)
  }

  fun getAllByProject(projectId: Long, pageable: Pageable): Page<ApiKey> {
    return apiKeyRepository.getAllByProjectId(projectId, pageable)
  }

  fun getApiKey(apiKey: String): Optional<ApiKey> {
    return apiKeyRepository.findByKey(apiKey)
  }

  fun getApiKey(id: Long): Optional<ApiKey> {
    return apiKeyRepository.findById(id)
  }

  fun deleteApiKey(apiKey: ApiKey) {
    apiKeyRepository.delete(apiKey)
  }

  fun getAvailableScopes(userAccountId: Long, project: Project): Set<ApiScope> {
    return permissionService.getProjectPermissionType(project.id, userAccountId)?.availableScopes?.toSet()
      ?: throw NotFoundException()
  }

  fun editApiKey(apiKey: ApiKey): ApiKey {
    return apiKeyRepository.save(apiKey)
  }

  fun deleteAllByProject(projectId: Long) {
    apiKeyRepository.deleteAllByProjectId(projectId)
  }

  fun saveAll(entities: Iterable<ApiKey>) {
    this.apiKeyRepository.saveAll(entities)
  }

  fun save(entity: ApiKey) {
    this.apiKeyRepository.save(entity)
  }
}
