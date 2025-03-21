package io.tolgee.service.project

import io.tolgee.activity.ActivityHolder
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.request.project.EditProjectRequest
import io.tolgee.dtos.request.project.ProjectFilters
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.dtos.response.ProjectDTO
import io.tolgee.dtos.response.ProjectDTO.Companion.fromEntityAndPermission
import io.tolgee.events.OnProjectSoftDeleted
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.ProjectNotFoundException
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.AvatarService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.security.PermissionService
import io.tolgee.util.Logging
import io.tolgee.util.SlugGenerator
import jakarta.persistence.EntityManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream

@Transactional
@Service
class ProjectService(
  private val projectRepository: ProjectRepository,
  private val entityManager: EntityManager,
  private val authenticationFacade: AuthenticationFacade,
  private val permissionService: PermissionService,
  private val namespaceService: NamespaceService,
  @Lazy
  private val languageService: LanguageService,
  private val currentDateProvider: CurrentDateProvider,
  private val applicationContext: ApplicationContext,
  private val avatarService: AvatarService,
  private val activityHolder: ActivityHolder,
  private val projectHolder: ProjectHolder,
  private val slugGenerator: SlugGenerator,
  @Lazy
  private val organizationService: OrganizationService,
) : Logging {
  @Transactional
  @Cacheable(cacheNames = [Caches.PROJECTS], key = "#id")
  fun findDto(id: Long): ProjectDto? {
    return projectRepository.find(id)?.let {
      ProjectDto.fromEntity(it)
    }
  }

  @Transactional
  @Cacheable(cacheNames = [Caches.PROJECTS], key = "#id")
  fun getDto(id: Long): ProjectDto {
    return findDto(id) ?: throw ProjectNotFoundException(id)
  }

  fun get(id: Long): Project {
    return find(id) ?: throw ProjectNotFoundException(id)
  }

  fun find(id: Long): Project? {
    return projectRepository.find(id)
  }

  fun findAll(ids: Iterable<Long>): List<Project> {
    return projectRepository.findAllById(ids)
  }

  fun findDeleted(id: Long): Project? {
    return projectRepository.findDeleted(id)
  }

  @Transactional
  fun getView(id: Long): ProjectWithLanguagesView {
    val perms = permissionService.getProjectPermissionData(id, authenticationFacade.authenticatedUser.id)
    val withoutPermittedLanguages =
      projectRepository.findViewById(authenticationFacade.authenticatedUser.id, id)
        ?: throw ProjectNotFoundException(id)
    return ProjectWithLanguagesView.fromProjectView(
      withoutPermittedLanguages,
      perms.directPermissions?.translateLanguageIds?.toList(),
    )
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
  fun editProject(
    id: Long,
    dto: EditProjectRequest,
  ): Project {
    val project =
      projectRepository.findById(id)
        .orElseThrow { NotFoundException() }!!

    if (!dto.useNamespaces && project.namespaces.isNotEmpty()) {
      throw ValidationException(Message.NAMESPACES_CANNOT_BE_DISABLED_WHEN_NAMESPACE_EXISTS)
    }

    project.name = dto.name
    project.description = dto.description
    project.icuPlaceholders = dto.icuPlaceholders
    project.useNamespaces = dto.useNamespaces

    if (project.defaultNamespace != null) {
      namespaceService.deleteUnusedNamespaces(listOf(project.defaultNamespace!!))
    }

    if (dto.defaultNamespaceId != null) {
      var namespace =
        project.namespaces.find { it.id == dto.defaultNamespaceId }
          ?: throw BadRequestException(Message.NAMESPACE_NOT_FROM_PROJECT)
      project.defaultNamespace = namespace
    } else {
      project.defaultNamespace = null
    }

    dto.baseLanguageId?.let {
      val language =
        project.languages.find { it.id == dto.baseLanguageId }
          ?: throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
      project.baseLanguage = language
      languageService.evictCacheForProject(project.id)
    }

    val newSlug = dto.slug
    if (newSlug != null && newSlug != project.slug) {
      validateSlugUniqueness(newSlug)
      project.slug = newSlug
    }

    // if project has null slag, generate it
    if (project.slug == null) {
      project.slug = generateSlug(project.name, null)
    }

    entityManager.persist(project)
    return project
  }

  fun findAllPermitted(userAccount: UserAccount): List<ProjectDTO> {
    return projectRepository.findAllPermitted(userAccount.id).asSequence()
      .map { result ->
        val project = result[0] as Project
        val permission = result[1] as Permission?
        val organization = result[2] as Organization
        val organizationRole = result[3] as OrganizationRole?
        val scopes =
          permissionService.computeProjectPermission(
            organizationRole?.type,
            organization.basePermission,
            permission,
            userAccount.role ?: UserAccount.Role.USER,
          ).scopes
        fromEntityAndPermission(project, scopes)
      }.toList()
  }

  fun findAllInOrganization(organizationId: Long): List<Project> {
    return this.projectRepository.findAllByOrganizationOwnerId(organizationId)
  }

  private fun addPermittedLanguagesToProjects(
    projectsPage: Page<ProjectView>,
    userId: Long,
  ): Page<ProjectWithLanguagesView> {
    val projectLanguageMap =
      permissionService.getPermittedTranslateLanguagesForProjectIds(
        projectsPage.content.map { it.id },
        userId,
      )
    val newContent =
      projectsPage.content.map {
        ProjectWithLanguagesView.fromProjectView(it, projectLanguageMap[it.id])
      }

    return PageImpl(newContent, projectsPage.pageable, projectsPage.totalElements)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#id")
  fun deleteProject(id: Long) {
    val project = get(id)
    val languages = project.languages
    val currentDate = currentDateProvider.date
    languages.forEach {
      it.deletedAt = currentDate
    }
    languageService.saveAll(languages)
    project.deletedAt = currentDate
    save(project)
    applicationContext.publishEvent(OnProjectSoftDeleted(project))
  }

  /**
   * If base language is missing on project it selects language with lowest id
   * It saves updated project and returns project's new baseLanguage
   */
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  fun getOrAssignBaseLanguage(projectId: Long): LanguageDto {
    return languageService.getProjectBaseLanguage(projectId)
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], allEntries = true)
  fun deleteAllByName(name: String) {
    projectRepository.findAllByName(name).forEach {
      this.deleteProject(it.id)
    }
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#project.id")
  fun removeAvatar(project: Project) {
    avatarService.removeAvatar(project)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#project.id")
  fun setAvatar(
    project: Project,
    avatar: InputStream,
  ) {
    avatarService.setAvatar(project, avatar)
  }

  fun validateSlugUniqueness(slug: String): Boolean {
    return projectRepository.countAllBySlug(slug) < 1
  }

  fun generateSlug(
    name: String,
    oldSlug: String? = null,
  ): String {
    return slugGenerator.generate(name, 3, 60) {
      if (oldSlug == it) {
        return@generate true
      }
      this.validateSlugUniqueness(it)
    }
  }

  fun findPermittedInOrganizationPaged(
    pageable: Pageable,
    search: String?,
    organizationId: Long? = null,
    filters: ProjectFilters? = null,
  ): Page<ProjectWithLanguagesView> {
    return findPermittedInOrganizationPaged(
      pageable = pageable,
      search = search,
      organizationId = organizationId,
      userAccountId = authenticationFacade.authenticatedUser.id,
      filters = filters,
    )
  }

  fun findPermittedInOrganizationPaged(
    pageable: Pageable,
    search: String?,
    organizationId: Long? = null,
    userAccountId: Long,
    filters: ProjectFilters? = ProjectFilters(),
  ): Page<ProjectWithLanguagesView> {
    val withoutPermittedLanguages =
      projectRepository.findAllPermitted(
        userAccountId,
        pageable,
        search,
        organizationId,
        filters ?: ProjectFilters(),
      )
    return addPermittedLanguagesToProjects(withoutPermittedLanguages, userAccountId)
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], allEntries = true)
  fun saveAll(projects: Collection<Project>): MutableList<Project> = projectRepository.saveAll(projects)

  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
  fun save(project: Project): Project {
    val isCreating = project.id == 0L
    projectRepository.save(project)
    if (isCreating) {
      projectHolder.project = ProjectDto.fromEntity(project)
      activityHolder.activityRevision.projectId = projectHolder.project.id
    }
    return project
  }

  fun refresh(project: Project): Project {
    if (project.id == 0L) {
      return project
    }
    return this.projectRepository.findById(project.id).orElseThrow { NotFoundException() }
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  fun transferToOrganization(
    projectId: Long,
    organizationId: Long,
  ) {
    val project = get(projectId)
    val organization = organizationService.find(organizationId) ?: throw NotFoundException()
    project.organizationOwner = organization
    save(project)
  }

  fun findAllByNameAndOrganizationOwner(
    name: String,
    organization: Organization,
  ): List<Project> {
    return projectRepository.findAllByNameAndOrganizationOwner(name, organization)
  }

  fun getProjectsWithDirectPermissions(
    id: Long,
    userIds: List<Long>,
  ): Map<Long, List<Project>> {
    val result = projectRepository.getProjectsWithDirectPermissions(id, userIds)
    return result
      .map { it[0] as Long to it[1] as Project }
      .groupBy { it.first }
      .mapValues { it.value.map { it.second } }
  }

  fun updateLastTaskNumber(
    projectId: Long,
    taskNumber: Long,
  ) {
    val project = get(projectId)
    project.lastTaskNumber = taskNumber
    projectRepository.saveAndFlush(project)
  }
}
