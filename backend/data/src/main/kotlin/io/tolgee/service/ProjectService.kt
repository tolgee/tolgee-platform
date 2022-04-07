package io.tolgee.service

import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.ProjectDto
import io.tolgee.dtos.query_results.ProjectStatistics
import io.tolgee.dtos.request.project.CreateProjectDTO
import io.tolgee.dtos.request.project.EditProjectDTO
import io.tolgee.dtos.response.ProjectDTO
import io.tolgee.dtos.response.ProjectDTO.Companion.fromEntityAndPermission
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.ProjectView
import io.tolgee.model.views.ProjectWithLanguagesView
import io.tolgee.repository.ProjectRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.util.SlugGenerator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import javax.persistence.EntityManager
import javax.persistence.criteria.Expression
import javax.persistence.criteria.JoinType
import javax.persistence.criteria.SetJoin

@Transactional
@Service
class ProjectService constructor(
  private val projectRepository: ProjectRepository,
  private val entityManager: EntityManager,
  private val screenshotService: ScreenshotService,
  private val organizationRoleService: OrganizationRoleService,
  private val authenticationFacade: AuthenticationFacade,
  private val slugGenerator: SlugGenerator,
  private val userAccountService: UserAccountService,
  private val avatarService: AvatarService,
  @Lazy
  private val projectHolder: ProjectHolder
) {
  @set:Autowired
  @set:Lazy
  lateinit var keyService: KeyService

  @set:Autowired
  @set:Lazy
  lateinit var organizationService: OrganizationService

  @set:Autowired
  @set:Lazy
  lateinit var languageService: LanguageService

  @set:Autowired
  @set:Lazy
  lateinit var translationService: TranslationService

  @set:Autowired
  @set:Lazy
  lateinit var importService: ImportService

  @set:Autowired
  @set:Lazy
  lateinit var mtServiceConfigService: MtServiceConfigService

  @set:Autowired
  @set:Lazy
  lateinit var securityService: SecurityService

  @set:Autowired
  @set:Lazy
  lateinit var permissionService: PermissionService

  @set:Autowired
  @set:Lazy
  lateinit var apiKeyService: ApiKeyService

  @Transactional
  @Cacheable(cacheNames = [Caches.PROJECTS], key = "#id")
  fun findDto(id: Long): ProjectDto? {
    return projectRepository.findById(id).orElse(null)?.let {
      ProjectDto.fromEntity(it)
    }
  }

  fun get(id: Long): Project {
    return projectRepository.findByIdOrNull(id) ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)
  }

  fun find(id: Long): Project? {
    return projectRepository.findByIdOrNull(id)
  }

  @Transactional
  fun getView(id: Long): ProjectWithLanguagesView {
    val perms = permissionService.getProjectPermissionData(id, authenticationFacade.userAccount.id)
    val withoutPermittedLanguages = projectRepository.findViewById(authenticationFacade.userAccount.id, id)
      ?: throw NotFoundException(Message.PROJECT_NOT_FOUND)
    return ProjectWithLanguagesView.fromProjectView(
      withoutPermittedLanguages,
      perms.directPermissions?.languageIds?.toList()
    )
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
  fun createProject(dto: CreateProjectDTO, userAccount: UserAccount? = null): Project {
    val project = Project()
    project.name = dto.name
    dto.organizationId?.also {
      organizationRoleService.checkUserIsOwner(it)
      project.organizationOwner = organizationService.find(it) ?: throw NotFoundException()

      if (dto.slug == null) {
        project.slug = generateSlug(dto.name, null)
      }
    } ?: let {
      project.userOwner = userAccount ?: authenticationFacade.userAccountEntity
      securityService.grantFullAccessToRepo(project, project.userOwner!!.id)
    }

    entityManager.persist(project)
    projectHolder.project = ProjectDto.fromEntity(project)

    val createdLanguages = dto.languages!!.map { languageService.createLanguage(it, project) }
    project.baseLanguage = getOrCreateBaseLanguage(dto, createdLanguages)

    return project
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
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
        val organization = result[2] as Organization?
        val organizationRole = result[3] as OrganizationRole?
        val permissionType = permissionService.computeProjectPermissionType(
          organizationRole?.type,
          organization?.basePermissions,
          permission?.type,
          permission?.languages?.map { it.id }?.toSet()
        ).type
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

  fun findAllInOrganization(organizationId: Long, pageable: Pageable, search: String?): Page<ProjectWithLanguagesView> {
    val withoutPermittedLanguages = this.projectRepository.findAllPermittedInOrganization(
      authenticationFacade.userAccount.id, organizationId, pageable, search
    )
    return addPermittedLanguagesToProjects(withoutPermittedLanguages)
  }

  fun addPermittedLanguagesToProjects(projectsPage: Page<ProjectView>): Page<ProjectWithLanguagesView> {
    val projectLanguageMap = permissionService.getPermittedTranslateLanguagesForProjectIds(
      projectsPage.content.map { it.id },
      authenticationFacade.userAccount.id
    )
    val newContent = projectsPage.content.map {
      ProjectWithLanguagesView.fromProjectView(it, projectLanguageMap[it.id])
    }

    return PageImpl(newContent, projectsPage.pageable, projectsPage.totalElements)
  }

  fun getProjectsStatistics(projectIds: Iterable<Long>): List<ProjectStatistics> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createTupleQuery()
    val root = query.from(Project::class.java)
    val languages = root.join(Project_.languages, JoinType.LEFT)
    val keys = root.join(Project_.keys, JoinType.LEFT)
    val stateJoins = mutableMapOf<TranslationState, SetJoin<Key, Translation>>()
    val stateSelects = linkedMapOf<TranslationState, Expression<Long>>()
    TranslationState.values().forEach { translationState ->
      val stateJoin = keys.join(Key_.translations, JoinType.LEFT)
      stateJoin.on(cb.equal(stateJoin.get(Translation_.state), translationState))
      stateJoins[translationState] = stateJoin
      stateSelects[translationState] = cb.countDistinct(stateJoin)
    }
    val keyCountSelect = cb.countDistinct(keys)
    val languageCountSelect = cb.countDistinct(languages)
    query.multiselect(
      root.get(Project_.id),
      keyCountSelect,
      languageCountSelect,
      *stateSelects.values.toTypedArray()
    )
    query.where(root.get(Project_.id).`in`(*projectIds.toList().toTypedArray()))
    query.groupBy(root.get(Project_.id))
    return entityManager.createQuery(query).resultList.map { tuple ->
      val stateMap = stateSelects.map { (state, select) ->
        state to tuple.get(select)
      }.toMap().toMutableMap()
      val untranslatedNotStored = tuple.get(languageCountSelect) * tuple.get(keyCountSelect) - stateMap.values.sum()
      stateMap[TranslationState.UNTRANSLATED] = (stateMap[TranslationState.UNTRANSLATED] ?: 0) + untranslatedNotStored
      ProjectStatistics(
        projectId = tuple.get(root.get(Project_.id)),
        languageCount = tuple.get(languageCountSelect),
        keyCount = tuple.get(keyCountSelect),
        translationStateCounts = stateMap,
      )
    }
  }

  fun getProjectsWithFetchedLanguages(projectIds: Iterable<Long>): List<Project> {
    return projectRepository.getWithLanguages(projectIds)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#id")
  fun deleteProject(id: Long) {
    val project = get(id)
    importService.getAllByProject(id).forEach {
      importService.deleteImport(it)
    }
    permissionService.deleteAllByProject(project.id)
    translationService.deleteAllByProject(project.id)
    screenshotService.deleteAllByProject(project.id)
    keyService.deleteAllByProject(project.id)
    apiKeyService.deleteAllByProject(project.id)
    languageService.deleteAllByProject(project.id)
    mtServiceConfigService.deleteAllByProjectId(project.id)
    avatarService.unlinkAvatarFiles(project)
    projectRepository.delete(project)
  }

  /**
   * If base language is missing on project it selects language with lowest id
   * It saves updated project and returns project's new baseLanguage
   */
  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  fun getOrCreateBaseLanguage(projectId: Long): Language? {
    return this.get(projectId).let { project ->
      project.baseLanguage ?: project.languages.toList().firstOrNull()?.let {
        project.baseLanguage = it
        projectRepository.save(project)
        it
      }
    }
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
  fun setAvatar(project: Project, avatar: InputStream) {
    avatarService.setAvatar(project, avatar)
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

  fun findPermittedPaged(pageable: Pageable, search: String?): Page<ProjectWithLanguagesView> {
    val withoutPermittedLanguages = projectRepository.findAllPermitted(
      authenticationFacade.userAccount.id,
      pageable,
      search
    )
    return addPermittedLanguagesToProjects(withoutPermittedLanguages)
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], allEntries = true)
  fun saveAll(projects: Collection<Project>): MutableList<Project> =
    projectRepository.saveAll(projects)

  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#result.id")
  fun save(project: Project): Project {
    return projectRepository.save(project)
  }

  fun refresh(project: Project): Project {
    if (project.id == 0L) {
      return project
    }
    return this.projectRepository.findById(project.id).orElseThrow { NotFoundException() }
  }

  private fun getOrCreateBaseLanguage(dto: CreateProjectDTO, createdLanguages: List<Language>): Language {
    if (dto.baseLanguageTag != null) {
      return createdLanguages.find { it.tag == dto.baseLanguageTag }
        ?: throw BadRequestException(Message.LANGUAGE_WITH_BASE_LANGUAGE_TAG_NOT_FOUND)
    }
    return createdLanguages[0]
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  fun transferToOrganization(projectId: Long, organizationId: Long) {
    val project = get(projectId)
    project.userOwner = null
    val organization = organizationService.find(organizationId) ?: throw NotFoundException()
    project.organizationOwner = organization
    save(project)
  }

  @CacheEvict(cacheNames = [Caches.PROJECTS], key = "#projectId")
  @Transactional
  fun transferToUser(projectId: Long, userId: Long) {
    val project = get(projectId)
    val userAccount = userAccountService[userId].orElseThrow { NotFoundException() }
    project.organizationOwner = null
    project.userOwner = userAccount
    save(project)
    permissionService.onProjectTransferredToUser(project, userAccount)
  }

  fun findAllByNameAndUserOwner(name: String, userOwner: UserAccount): List<Project> {
    return projectRepository.findAllByNameAndUserOwner(name, userOwner)
  }
}
