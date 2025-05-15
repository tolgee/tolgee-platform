package io.tolgee.service.language

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.component.CurrentDateProvider
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.cacheable.OrganizationLanguageDto
import io.tolgee.dtos.request.LanguageRequest
import io.tolgee.dtos.request.language.LanguageFilters
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.repository.LanguageRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class LanguageService(
  private val languageRepository: LanguageRepository,
  private val entityManager: EntityManager,
  private val projectService: ProjectService,
  private val permissionService: PermissionService,
  @Lazy
  private val securityService: SecurityService,
  @Lazy
  private val autoTranslationService: AutoTranslationService,
  @Suppress("SelfReferenceConstructorParameter")
  @Lazy
  private val self: LanguageService,
  private val cacheManager: org.springframework.cache.CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val importService: ImportService,
  private val activityHolder: ActivityHolder,
  private val authenticationFacade: AuthenticationFacade,
  private val applicationContext: ApplicationContext,
) {
  @set:Autowired
  @set:Lazy
  lateinit var translationService: TranslationService

  @Transactional
  fun createLanguage(
    dto: LanguageRequest?,
    project: Project,
  ): Language {
    val language = fromRequestDTO(dto!!)
    language.project = project
    projectService.refresh(project).languages.add(language)
    languageRepository.save(language)
    evictCache(language)
    return language
  }

  @Transactional
  fun deleteLanguage(
    languageId: Long,
    projectId: Long,
  ) {
    entityManager.clear()
    val language = getEntity(languageId, projectId)
    deleteLanguage(language)
  }

  @Transactional
  fun deleteLanguage(id: Long) {
    deleteLanguage(getEntity(id))
  }

  @Transactional
  fun deleteLanguage(language: Language) {
    language.deletedAt = currentDateProvider.date
    importService.onExistingLanguageRemoved(language)
    permissionService.removeLanguageFromPermissions(language)
    save(language)
    self.hardDeleteLanguageAsync(language, authenticationFacade.authenticatedUserOrNull?.id)
  }

  @Async
  @Transactional
  fun hardDeleteLanguageAsync(
    language: Language,
    authorId: Long?,
  ) {
    activityHolder.activity = ActivityType.HARD_DELETE_LANGUAGE
    activityHolder.activityRevision.authorId = authorId
    activityHolder.activityRevision.projectId = language.project.id
    hardDeleteLanguage(language)
  }

  @Transactional
  fun hardDeleteLanguage(language: Language) {
    LanguageHardDeleter(language, applicationContext).delete()
    evictCache(language)
  }

  @Transactional
  fun hardDeleteLanguage(id: Long) {
    hardDeleteLanguage(getEntity(id))
  }

  @Transactional
  fun editLanguage(
    languageId: Long,
    projectId: Long,
    dto: LanguageRequest,
  ): Language {
    val language = getEntity(languageId, projectId)
    language.updateByDTO(dto)
    entityManager.persist(language)
    evictCache(language)
    return language
  }

  fun getImplicitLanguages(
    projectId: Long,
    userId: Long,
  ): Set<LanguageDto> {
    val all = self.getProjectLanguages(projectId)
    val viewLanguageIds =
      permissionService.getProjectPermissionData(
        projectId,
        userId,
      ).computedPermissions.viewLanguageIds

    val permitted =
      if (viewLanguageIds.isNullOrEmpty()) {
        all
      } else {
        all.filter { viewLanguageIds.contains(it.id) }
      }

    return permitted
      .sortedBy { it.id }
      // base first
      .sortedBy { if (it.base) 0 else 1 }
      .take(2).toSet()
  }

  @Transactional
  fun findAll(projectId: Long): Set<LanguageDto> {
    return self.getProjectLanguages(projectId).toSet()
  }

  fun get(
    languageId: Long,
    projectId: Long,
  ): LanguageDto {
    return find(languageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  fun find(
    languageId: Long,
    projectId: Long,
  ): LanguageDto? {
    return self.getProjectLanguages(projectId).singleOrNull { it.id == languageId }
  }

  fun findEntity(id: Long): Language? {
    return languageRepository.find(id)
  }

  fun getEntity(id: Long): Language {
    return this.findEntity(id) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  fun getEntity(
    languageId: Long,
    projectId: Long,
  ): Language {
    return findEntity(languageId, projectId) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  fun findEntity(
    languageId: Long,
    projectId: Long,
  ): Language? {
    return languageRepository.find(languageId, projectId)
  }

  fun findByTag(
    tag: String,
    project: Project,
  ): LanguageDto? {
    return self.getProjectLanguages(project.id).singleOrNull { tag == it.tag }
  }

  fun getByTag(
    tag: String,
    project: Project,
  ): LanguageDto {
    return findByTag(tag, project) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  fun findByTag(
    tag: String?,
    projectId: Long,
  ): LanguageDto? {
    return self.getProjectLanguages(projectId).singleOrNull { tag == it.tag }
  }

  fun findByTags(
    tags: Collection<String>,
    projectId: Long,
  ): Set<LanguageDto> {
    val languages = self.getProjectLanguages(projectId).filter { tags.contains(it.tag) }
    val sortedByTagsParam =
      languages.sortedBy { language ->
        tags.indexOfFirst { tag -> language.tag == tag }
      }
    return sortedByTagsParam.toSet()
  }

  fun findEntitiesByTags(
    tags: Collection<String>,
    projectId: Long,
  ): Set<Language> {
    val languages = languageRepository.findAllByTagInAndProjectId(tags, projectId)
    val sortedByTagsParam =
      languages.sortedBy { language ->
        tags.indexOfFirst { tag -> language.tag == tag }
      }
    return sortedByTagsParam.toSet()
  }

  @Transactional
  fun getLanguagesForExport(
    languages: Set<String>?,
    projectId: Long,
    userId: Long,
  ): Set<LanguageDto> {
    if (languages == null) {
      return permissionService.getPermittedViewLanguages(projectId, userId).toSet()
    } else {
      securityService.checkLanguageViewPermissionByTag(projectId, languages)
      return findByTags(languages, projectId)
    }
  }

  @Transactional
  fun getLanguagesForTranslationsView(
    languages: Set<String>?,
    projectId: Long,
    userId: Long,
  ): Set<LanguageDto> {
    val canViewTranslations = securityService.currentPermittedScopesContain(Scope.TRANSLATIONS_VIEW)

    if (!canViewTranslations) {
      return emptySet()
    }
    return if (languages == null) {
      getImplicitLanguages(projectId, userId)
    } else {
      findByTagsAndFilterPermitted(projectId, userId, languages)
    }
  }

  private fun findByTagsAndFilterPermitted(
    projectId: Long,
    userId: Long,
    languages: Set<String>,
  ): Set<LanguageDto> {
    val viewLanguageIds =
      permissionService.getProjectPermissionData(
        projectId,
        userId,
      ).computedPermissions.viewLanguageIds
    return if (viewLanguageIds.isNullOrEmpty()) {
      findByTags(languages, projectId)
    } else {
      findByTags(languages, projectId).filter { viewLanguageIds.contains(it.id) }.toSet()
    }
  }

  fun findByName(
    name: String?,
    project: Project,
  ): Optional<Language> {
    return languageRepository.findByNameAndProject(name, project)
  }

  fun deleteAllByProject(projectId: Long) {
    translationService.deleteAllByProject(projectId)
    autoTranslationService.deleteConfigsByProject(projectId)
    entityManager.createNativeQuery(
      "delete from language_stats " +
        "where language_id in (select id from language where project_id = :projectId)",
    )
      .setParameter("projectId", projectId)
      .executeUpdate()
    entityManager.createNativeQuery("DELETE FROM language WHERE project_id = :projectId")
      .setParameter("projectId", projectId)
      .executeUpdate()
    evictCacheForProject(projectId)
  }

  fun save(language: Language): Language {
    evictCache(language)
    return this.languageRepository.save(language)
  }

  fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
    evictCache(languages)
    return this.languageRepository.saveAll(languages)
  }

  private fun evictCache(languages: Iterable<Language>) {
    languages.map { it.project.id }.toSet().forEach {
      evictCacheForProject(it)
    }
  }

  fun evictCacheForProject(projectId: Long) {
    cacheManager.getCache(Caches.LANGUAGES)?.evict(projectId)
  }

  private fun evictCache(language: Language) {
    cacheManager.getCache(Caches.LANGUAGES)?.evict(language.project.id)
  }

  fun getPaged(
    projectId: Long,
    pageable: Pageable,
    filters: LanguageFilters?,
  ): Page<LanguageDto> {
    return this.languageRepository.findAllByProjectId(projectId, pageable, filters ?: LanguageFilters())
  }

  fun getPagedByOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<OrganizationLanguageDto> {
    return this.languageRepository.findAllByOrganizationId(organizationId, pageable, search)
  }

  fun findByIdIn(ids: Iterable<Long>): List<Language> {
    return languageRepository.findAllById(ids)
  }

  fun getLanguageIdsByTags(
    projectId: Long,
    languageTags: Collection<String>,
  ): Map<String, Language> {
    return languageRepository.findAllByTagInAndProjectId(languageTags, projectId).associateBy { it.tag }
  }

  @Transactional
  fun getDtosOfProjects(projectIds: List<Long>): Map<Long, List<LanguageDto>> {
    return projectIds.associateWith { self.getProjectLanguages(it) }
  }

  @Cacheable(Caches.LANGUAGES, key = "#projectId")
  @Transactional
  fun getProjectLanguages(projectId: Long): List<LanguageDto> {
    val cache = cacheManager.getCache(Caches.LANGUAGES)
    val list =
      (
        (cache?.get(projectId)?.get() as? List<LanguageDto>)
          ?: languageRepository.findAllDtosByProjectId(projectId)
      ).toMutableList()

    handleMissingBaseLanguage(list, projectId)
    sortLanguages(list)
    cache?.put(projectId, list)
    return list
  }

  private fun sortLanguages(list: MutableList<LanguageDto>): MutableList<LanguageDto> {
    list.sortBy { it.tag }
    list.sortBy { !it.base }
    return list
  }

  private fun handleMissingBaseLanguage(
    all: MutableList<LanguageDto>,
    projectId: Long,
  ) {
    if (all.none { it.base }) {
      val newBase = setNewProjectBaseLanguage(all, projectId)
      all.find { it.id == newBase.id }?.also { it.base = true } ?: let {
        all.add(newBase)
      }
    }
    sortLanguages(all)
  }

  @Transactional
  fun getProjectBaseLanguage(projectId: Long): LanguageDto {
    val projectLanguages = self.getProjectLanguages(projectId)
    return projectLanguages.singleOrNull { it.base } ?: let {
      setNewProjectBaseLanguage(projectLanguages, projectId)
    }
  }

  fun setNewProjectBaseLanguage(project: Project) {
    val projectLanguages = self.getProjectLanguages(project.id)
    val newBase = projectLanguages.firstOrNull() ?: self.createDefaultLanguage(project.id)
    project.baseLanguage = entityManager.getReference(Language::class.java, newBase.id)
    projectService.save(project)
    evictCacheForProject(project.id)
  }

  private fun setNewProjectBaseLanguage(
    projectLanguages: List<LanguageDto>,
    projectId: Long,
  ): LanguageDto {
    val newBase =
      projectLanguages.find { it.tag == "en" }
        ?: projectLanguages.firstOrNull()
        ?: self.createDefaultLanguage(projectId)
    val project = projectService.get(projectId)
    project.baseLanguage = entityManager.getReference(Language::class.java, newBase.id)
    projectService.save(project)
    evictCacheForProject(projectId)
    return LanguageDto.fromEntity(newBase, newBase.id)
  }

  @Transactional
  fun dtosFromEntities(
    entities: List<Language>,
    projectId: Long,
  ): List<LanguageDto> {
    val all = self.getProjectLanguages(projectId)
    return entities.map { entity ->
      all.find { it.id == entity.id } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
    }
  }

  @CacheEvict(Caches.LANGUAGES, key = "#projectId")
  fun createDefaultLanguage(projectId: Long): Language {
    val language = Language()
    language.name = "English"
    language.originalName = "English"
    language.flagEmoji = "🇬🇧"
    language.tag = "en"
    language.project = entityManager.getReference(Project::class.java, projectId)
    return languageRepository.save(language)
  }
}
