package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.model.views.LanguageView
import io.tolgee.repository.LanguageRepository
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.TranslationService
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
) {
  @set:Autowired
  @set:Lazy
  lateinit var translationService: TranslationService

  @Transactional
  fun createLanguage(dto: LanguageDto?, project: Project): Language {
    val language = fromRequestDTO(dto!!)
    language.project = project
    projectService.refresh(project).languages.add(language)
    languageRepository.save(language)
    return language
  }

  @Transactional
  fun deleteLanguage(id: Long) {
    val language = languageRepository.findById(id).orElseThrow { NotFoundException() }
    permissionService.removeLanguageFromPermissions(language)
    languageRepository.delete(language)
    entityManager.flush()
  }

  @Transactional
  fun editLanguage(id: Long, dto: LanguageDto): Language {
    val language = languageRepository.findById(id).orElseThrow { NotFoundException() }
    return editLanguage(language, dto)
  }

  @Transactional
  fun editLanguage(language: Language, dto: LanguageDto): Language {
    language.updateByDTO(dto)
    entityManager.persist(language)
    return language
  }

  fun getImplicitLanguages(projectId: Long, userId: Long): Set<Language> {
    val all = languageRepository.findAllByProjectId(projectId)
    val viewLanguageIds = permissionService.getProjectPermissionData(
      projectId,
      userId
    ).computedPermissions.viewLanguageIds

    val permitted = if (viewLanguageIds.isNullOrEmpty())
      all
    else
      all.filter { viewLanguageIds.contains(it.id) }

    return permitted.sortedBy { it.id }.take(2).toSet()
  }

  @Transactional
  fun findAll(projectId: Long): Set<Language> {
    return languageRepository.findAllByProjectId(projectId).toSet()
  }

  fun get(id: Long): Language {
    return find(id) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  @Transactional
  fun getView(id: Long): LanguageView {
    return findView(id) ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  @Transactional
  fun findView(id: Long): LanguageView? {
    return languageRepository.findView(id)
  }

  fun find(id: Long): Language? {
    return languageRepository.findById(id).orElse(null)
  }

  @Deprecated("use find method", replaceWith = ReplaceWith("find"))
  fun findById(id: Long): Optional<Language> {
    return languageRepository.findById(id)
  }

  fun findByTag(tag: String, project: Project): Optional<Language> {
    return languageRepository.findByTagAndProject(tag, project)
  }

  fun findByTag(tag: String?, projectId: Long): Optional<Language> {
    return languageRepository.findByTagAndProjectId(tag, projectId)
  }

  fun findByTags(tags: Collection<String>, projectId: Long): Set<Language> {
    val languages = languageRepository.findAllByTagInAndProjectId(tags, projectId)
    val sortedByTagsParam = languages.sortedBy { language ->
      tags.indexOfFirst { tag -> language.tag == tag }
    }
    return sortedByTagsParam.toSet()
  }

  @Transactional
  fun getLanguagesForExport(languages: Set<String>?, projectId: Long, userId: Long): Set<Language> {
    if (languages == null) {
      return permissionService.getPermittedViewLanguages(projectId, userId).toSet()
    } else {
      securityService.checkLanguageViewPermissionByTag(projectId, languages)
      return findByTags(languages, projectId)
    }
  }

  @Transactional
  fun getLanguagesForTranslationsView(languages: Set<String>?, projectId: Long, userId: Long): Set<Language> {
    val canViewTranslations =
      permissionService.getProjectPermissionScopes(projectId, userId)?.contains(Scope.TRANSLATIONS_VIEW) == true

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
    languages: Set<String>
  ): Set<Language> {
    val viewLanguageIds = permissionService.getProjectPermissionData(
      projectId,
      userId
    ).computedPermissions.viewLanguageIds
    return if (viewLanguageIds.isNullOrEmpty()) {
      findByTags(languages, projectId)
    } else {
      findByTags(languages, projectId).filter { viewLanguageIds.contains(it.id) }.toSet()
    }
  }

  fun findByName(name: String?, project: Project): Optional<Language> {
    return languageRepository.findByNameAndProject(name, project)
  }

  fun deleteAllByProject(projectId: Long) {
    translationService.deleteAllByProject(projectId)
    autoTranslationService.deleteConfigsByProject(projectId)
    entityManager.createNativeQuery(
      "delete from language_stats " +
        "where language_id in (select id from language where project_id = :projectId)"
    )
      .setParameter("projectId", projectId)
      .executeUpdate()
    entityManager.createNativeQuery("DELETE FROM language WHERE project_id = :projectId")
      .setParameter("projectId", projectId)
      .executeUpdate()
  }

  fun save(language: Language): Language {
    return this.languageRepository.save(language)
  }

  fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
    return this.languageRepository.saveAll(languages)
  }

  fun getPaged(projectId: Long, pageable: Pageable): Page<LanguageView> {
    return this.languageRepository.findAllByProjectId(projectId, pageable)
  }

  fun findByIdIn(ids: Iterable<Long>): List<Language> {
    return languageRepository.findAllById(ids)
  }

  fun getLanguageIdsByTags(projectId: Long, languageTags: Collection<String>): Map<String, Language> {
    return languageRepository.findAllByTagInAndProjectId(languageTags, projectId).associateBy { it.tag }
  }

  @Transactional
  fun getViewsOfProjects(projectIds: List<Long>): List<LanguageView> {
    return languageRepository.getViewsOfProjects(projectIds)
  }
}
