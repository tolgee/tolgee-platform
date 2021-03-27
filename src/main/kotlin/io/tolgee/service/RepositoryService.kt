package io.tolgee.service

import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Permission
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.RepositoryRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors
import javax.persistence.EntityManager

@Service
open class RepositoryService constructor(
        private val repositoryRepository: RepositoryRepository,
        private val entityManager: EntityManager,
        private val securityService: SecurityService,
        private val permissionRepository: PermissionRepository,
        private val permissionService: PermissionService,
        private val apiKeyService: ApiKeyService,
        private val screenshotService: ScreenshotService
) {
    private var keyService: KeyService? = null


    @set:Autowired
    lateinit var languageService: LanguageService

    @set:Autowired
    lateinit var translationService: TranslationService

    @Transactional
    open fun get(id: Long): Optional<Repository> {
        return repositoryRepository.findById(id)
    }

    @Transactional
    open fun createRepository(dto: CreateRepositoryDTO): Repository {
        val repository = Repository()
        repository.name = dto.name
        securityService.grantFullAccessToRepo(repository)
        for (language in dto.languages!!) {
            languageService.createLanguage(language, repository)
        }
        entityManager.persist(repository)
        return repository
    }

    @Transactional
    open fun editRepository(dto: EditRepositoryDTO): Repository {
        val repository = repositoryRepository.findById(dto.repositoryId!!)
                .orElseThrow { NotFoundException() }
        repository.name = dto.name
        entityManager.persist(repository)
        return repository
    }

    @Transactional
    open fun findAllPermitted(userAccount: UserAccount?): Set<RepositoryDTO> {
        return permissionRepository.findAllByUser(userAccount).stream()
                .map { permission: Permission -> fromEntityAndPermission(permission.repository!!, permission) }
                .collect(Collectors.toCollection { LinkedHashSet() })
    }

    @Transactional
    open fun deleteRepository(id: Long) {
        val repository = get(id).orElseThrow { NotFoundException() }
        permissionService.deleteAllByRepository(repository.id)
        translationService.deleteAllByRepository(repository.id)
        screenshotService.deleteAllByRepository(repository.id)
        keyService!!.deleteAllByRepository(repository.id)
        apiKeyService.deleteAllByRepository(repository.id)
        languageService.deleteAllByRepository(repository.id)
        repositoryRepository.delete(repository)
    }

    @Autowired
    open fun setKeyService(keyService: KeyService?) {
        this.keyService = keyService
    }
}
