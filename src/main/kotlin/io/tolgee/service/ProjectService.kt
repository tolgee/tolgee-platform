package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.CreateProjectDTO
import io.tolgee.dtos.request.EditProjectDTO
import io.tolgee.dtos.response.ProjectDTO
import io.tolgee.dtos.response.ProjectDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.*
import io.tolgee.model.views.ProjectView
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.dataImport.ImportService
import io.tolgee.util.SlugGenerator
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
  private val permissionService: PermissionService,
  private val apiKeyService: ApiKeyService,
  private val screenshotService: ScreenshotService,
  private val organizationRoleService: OrganizationRoleService,
  private val authenticationFacade: AuthenticationFacade,
  private val slugGenerator: SlugGenerator,
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
  fun createProject(dto: CreateProjectDTO): Project {
    val project = Project()
    project.name = dto.name
    dto.organizationId?.also {
      organizationRoleService.checkUserIsOwner(it)
      project.organizationOwner = organizationService.get(it) ?: throw NotFoundException()

      if (dto.slug == null) {
        project.slug = generateSlug(dto.name!!, null)
      }
    } ?: let {
      project.userOwner = authenticationFacade.userAccount
      securityService.grantFullAccessToRepo(project)
    }

    val createdLanguages = dto.languages!!.map { languageService.createLanguage(it, project) }
    project.baseLanguage = getBaseLanguage(dto, createdLanguages)

    entityManager.persist(project)
    return project
  }

  @Transactional
  fun editProject(id: Long, dto: EditProjectDTO): Project {
    val project = projectRepository.findById(id)
      .orElseThrow { NotFoundException() }!!
    project.name = dto.name

    dto.baseLanguageId?.let {
      val language = project.languages.find { it.id == dto.baseLanguageId }
        ?: throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
      project.baseLanguage = language
    }

    val newSlug = dto.slug
    if (newSlug != null && newSlug != project.slug) {
      validateSlugUniqueness(newSlug)
      project.slug = newSlug
    }

    // if project has null slag, generate it
    if (project.slug == null) {
      project.slug = generateSlug(project.name!!, null)
    }

    entityManager.persist(project)
    return project
  }

  fun findAllPermitted(userAccount: UserAccount): List<ProjectDTO> {
    return projectRepository.findAllPermitted(userAccount.id!!).asSequence()
      .map { result ->
        val project = result[0] as Project
        val permission = result[1] as Permission?
        val organization = result[2] as Organization?
        val organizationRole = result[3] as OrganizationRole?
        val permissionType = permissionService.computeProjectPermissionType(
          organizationRole?.type,
          organization?.basePermissions,
          permission?.type
        )
          ?: throw IllegalStateException(
            "Project project should not" +
              " return project with no permission for provided user"
          )

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
  fun deleteProject(id: Long) {
    val project = get(id).orElseThrow { NotFoundException() }!!
    importService.getAllByProject(id).forEach {
      importService.deleteImport(it)
    }
    permissionService.deleteAllByProject(project.id)
    translationService.deleteAllByProject(project.id)
    screenshotService.deleteAllByProject(project.id)
    keyService.deleteAllByProject(project.id)
    apiKeyService.deleteAllByProject(project.id)
    languageService.deleteAllByProject(project.id)
    projectRepository.delete(project)
  }

  /**
   * If base language is missing on project it selects language with lowest id
   * It saves updated project and returns project's new baseLanguage
   */
  fun autoSetBaseLanguage(projectId: Long): Language? {
    return this.get(projectId).orElse(null)?.let { project ->
      project.baseLanguage ?: project.languages.toList().firstOrNull()?.let {
        project.baseLanguage = it
        projectRepository.save(project)
        it
      }
    }
    return null
  }

  fun deleteAllByName(name: String) {
    projectRepository.findAllByName(name).forEach {
      this.deleteProject(it.id)
    }
  }

  fun validateSlugUniqueness(slug: String): Boolean {
    return projectRepository.countAllBySlug(slug) < 1
  }

  fun generateSlug(name: String, oldSlug: String? = null): String {
    return slugGenerator.generate(name, 3, 60) {
      if (oldSlug == it) {
        return@generate true
      }
      this.validateSlugUniqueness(it)
    }
  }

  fun findPermittedPaged(pageable: Pageable, search: String?): Page<ProjectView> {
    return projectRepository.findAllPermitted(authenticationFacade.userAccount.id!!, pageable, search)
  }

  fun saveAll(projects: Collection<Project>): MutableList<Project> =
    projectRepository.saveAll(projects)

  fun save(project: Project): Project = projectRepository.save(project)

  private fun getBaseLanguage(dto: CreateProjectDTO, createdLanguages: List<Language>): Language {
    if (dto.baseLanguageTag != null) {
      return createdLanguages.find { it.tag == dto.baseLanguageTag }
        ?: throw BadRequestException(Message.LANGUAGE_WITH_BASE_LANGUAGE_TAG_NOT_FOUND)
    }
    return createdLanguages[0]
  }
}
