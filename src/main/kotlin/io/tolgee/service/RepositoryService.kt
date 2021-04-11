package io.tolgee.service

import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.*
import io.tolgee.model.views.RepositoryView
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.RepositoryRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.AddressPartGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Transactional
@Service
open class RepositoryService constructor(
        private val repositoryRepository: RepositoryRepository,
        private val entityManager: EntityManager,
        private val securityService: SecurityService,
        private val permissionRepository: PermissionRepository,
        private val permissionService: PermissionService,
        private val apiKeyService: ApiKeyService,
        private val screenshotService: ScreenshotService,
        private val organizationRoleService: OrganizationRoleService,
        private val authenticationFacade: AuthenticationFacade,
        private val addressPartGenerator: AddressPartGenerator

) {
    private var keyService: KeyService? = null


    @set:Autowired
    lateinit var organizationService: OrganizationService

    @set:Autowired
    lateinit var languageService: LanguageService

    @set:Autowired
    lateinit var translationService: TranslationService

    @Transactional
    open fun get(id: Long): Optional<Repository?> {
        return repositoryRepository.findById(id)
    }

    @Transactional
    open fun getView(id: Long): RepositoryView? {
        return repositoryRepository.findViewById(authenticationFacade.userAccount.id!!, id)
    }

    @Transactional
    open fun createRepository(dto: CreateRepositoryDTO): Repository {
        val repository = Repository()
        repository.name = dto.name
        dto.organizationId?.also {
            organizationRoleService.checkUserIsOwner(it)
            repository.organizationOwner = organizationService.get(it) ?: throw NotFoundException()

            if (dto.addressPart == null) {
                repository.addressPart = generateAddressPart(dto.name!!, null)
            }

        } ?: let {
            repository.userOwner = authenticationFacade.userAccount
            securityService.grantFullAccessToRepo(repository)
        }
        for (language in dto.languages!!) {
            languageService.createLanguage(language, repository)
        }

        entityManager.persist(repository)
        return repository
    }

    @Transactional
    open fun editRepository(dto: EditRepositoryDTO): Repository {
        val repository = repositoryRepository.findById(dto.repositoryId!!)
                .orElseThrow { NotFoundException() }!!
        repository.name = dto.name
        entityManager.persist(repository)
        return repository
    }

    open fun findAllPermitted(userAccount: UserAccount): List<RepositoryDTO> {
        return repositoryRepository.findAllPermitted(userAccount.id!!).asSequence()
                .map { result ->
                    val repository = result[0] as Repository
                    val permission = result[1] as Permission?
                    val organization = result[2] as Organization?
                    val organizationRole = result[3] as OrganizationRole?
                    val permissionType = permissionService.computeRepositoryPermissionType(
                            organizationRole?.type,
                            organization?.basePermissions,
                            permission?.type
                    )
                            ?: throw IllegalStateException("Repository repository should not return repository with no permission for provided user")

                    fromEntityAndPermission(repository, permissionType)
                }.toList()
    }

    open fun findAllInOrganization(organizationId: Long): List<Repository> {
        return this.repositoryRepository.findAllByOrganizationOwnerId(organizationId)
    }

    open fun findAllInOrganization(organizationId: Long, pageable: Pageable): Page<RepositoryView> {
        return this.repositoryRepository.findAllPermittedInOrganization(authenticationFacade.userAccount.id!!, organizationId, pageable)
    }

    @Transactional
    open fun deleteRepository(id: Long) {
        val repository = get(id).orElseThrow { NotFoundException() }!!
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

    open fun validateAddressPartUniqueness(addressPart: String): Boolean {
        return repositoryRepository.countAllByAddressPart(addressPart) < 1
    }

    open fun generateAddressPart(name: String, oldAddressPart: String?): String {
        return addressPartGenerator.generate(name, 3, 60) {
            if (oldAddressPart == it) {
                return@generate true
            }
            this.validateAddressPartUniqueness(it)
        }
    }

    open fun findPermittedPaged(pageable: Pageable): Page<RepositoryView> {
        return repositoryRepository.findAllPermitted(authenticationFacade.userAccount.id!!, pageable)
    }
}
