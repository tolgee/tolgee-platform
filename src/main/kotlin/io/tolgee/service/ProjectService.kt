package io.tolgee.service

import io.tolgee.dtos.request.CreateRepositoryDTO
import io.tolgee.dtos.request.EditRepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO
import io.tolgee.dtos.response.RepositoryDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.*
import io.tolgee.model.views.ProjectView
import io.tolgee.repository.PermissionRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.dataImport.ImportService
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
class ProjectService constructor(
        private val projectRepository: ProjectRepository,
        private val entityManager: EntityManager,
        private val securityService: SecurityService,
        private val permissionRepository: PermissionRepository,
        private val permissionService: PermissionService,
        private val apiKeyService: ApiKeyService,
        private val screenshotService: ScreenshotService,
        private val organizationRoleService: OrganizationRoleService,
        private val authenticationFacade: AuthenticationFacade,
        private val addressPartGenerator: AddressPartGenerator,
) {
    @set:Autowired
    lateinit var keyService: KeyService

    @set:Autowired
    lateinit var organizationService: OrganizationService

    @set:Autowired
    lateinit var languageService: LanguageService

    @set:Autowired
    lateinit var translationService: TranslationService

    @set:Autowired
    lateinit var importService: ImportService

    @Transactional
    fun get(id: Long): Optional<Project?> {
        return projectRepository.findById(id)
    }

    @Transactional
    fun getView(id: Long): ProjectView? {
        return projectRepository.findViewById(authenticationFacade.userAccount.id!!, id)
    }

    @Transactional
    fun createRepository(dto: CreateRepositoryDTO): Project {
        val project = Project()
        project.name = dto.name
        dto.organizationId?.also {
            organizationRoleService.checkUserIsOwner(it)
            project.organizationOwner = organizationService.get(it) ?: throw NotFoundException()

            if (dto.addressPart == null) {
                project.addressPart = generateAddressPart(dto.name!!, null)
            }

        } ?: let {
            project.userOwner = authenticationFacade.userAccount
            securityService.grantFullAccessToRepo(project)
        }
        for (language in dto.languages!!) {
            languageService.createLanguage(language, project)
        }

        entityManager.persist(project)
        return project
    }

    @Transactional
    fun editRepository(dto: EditRepositoryDTO): Project {
        val project = projectRepository.findById(dto.projectId!!)
                .orElseThrow { NotFoundException() }!!
        project.name = dto.name
        entityManager.persist(project)
        return project
    }

    fun findAllPermitted(userAccount: UserAccount): List<RepositoryDTO> {
        return projectRepository.findAllPermitted(userAccount.id!!).asSequence()
                .map { result ->
                    val project = result[0] as Project
                    val permission = result[1] as Permission?
                    val organization = result[2] as Organization?
                    val organizationRole = result[3] as OrganizationRole?
                    val permissionType = permissionService.computeRepositoryPermissionType(
                            organizationRole?.type,
                            organization?.basePermissions,
                            permission?.type
                    )
                            ?: throw IllegalStateException("Repository project should not" +
                                    " return project with no permission for provided user")

                    fromEntityAndPermission(project, permissionType)
                }.toList()
    }

    fun findAllInOrganization(organizationId: Long): List<Project> {
        return this.projectRepository.findAllByOrganizationOwnerId(organizationId)
    }

    fun findAllInOrganization(organizationId: Long, pageable: Pageable, search: String?): Page<ProjectView> {
        return this.projectRepository
                .findAllPermittedInOrganization(
                        authenticationFacade.userAccount.id!!, organizationId, pageable, search
                )
    }

    @Transactional
    fun deleteRepository(id: Long) {
        val project = get(id).orElseThrow { NotFoundException() }!!
        importService.getAllByRepository(id).forEach {
            importService.deleteImport(it)
        }
        permissionService.deleteAllByRepository(project.id)
        translationService.deleteAllByRepository(project.id)
        screenshotService.deleteAllByRepository(project.id)
        keyService.deleteAllByRepository(project.id)
        apiKeyService.deleteAllByRepository(project.id)
        languageService.deleteAllByRepository(project.id)
        projectRepository.delete(project)
    }

    fun deleteAllByName(name: String) {
        projectRepository.findAllByName(name).forEach {
            this.deleteRepository(it.id)
        }
    }

    fun validateAddressPartUniqueness(addressPart: String): Boolean {
        return projectRepository.countAllByAddressPart(addressPart) < 1
    }

    fun generateAddressPart(name: String, oldAddressPart: String? = null): String {
        return addressPartGenerator.generate(name, 3, 60) {
            if (oldAddressPart == it) {
                return@generate true
            }
            this.validateAddressPartUniqueness(it)
        }
    }

    fun findPermittedPaged(pageable: Pageable, search: String?): Page<ProjectView> {
        return projectRepository.findAllPermitted(authenticationFacade.userAccount.id!!, pageable, search)
    }

    fun saveAll(projects: Collection<Project>): MutableList<Project> =
            projectRepository.saveAll(projects)
}
