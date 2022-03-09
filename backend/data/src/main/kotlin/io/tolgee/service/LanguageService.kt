package io.tolgee.service

import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Project
import io.tolgee.repository.LanguageRepository
import io.tolgee.service.machineTranslation.MtServiceConfigService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
class LanguageService(
  private val languageRepository: LanguageRepository,
  private val entityManager: EntityManager,
  private val projectService: ProjectService,
  private val permissionService: PermissionService
) {
  @set:Autowired
  @set:Lazy
  lateinit var translationService: TranslationService

  @set:Autowired
  @set:Lazy
  lateinit var mtServiceConfigService: MtServiceConfigService

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
    translationService.deleteAllByLanguage(language.id)
    mtServiceConfigService.deleteAllByTargetLanguageId(language.id)
    permissionService.onLanguageDeleted(language)
    languageRepository.delete(language)
  }

  @Transactional
  fun editLanguage(id: Long, dto: LanguageDto): Language {
    val language = languageRepository.findById(id).orElseThrow { NotFoundException() }
    language.updateByDTO(dto)
    entityManager.persist(language)
    return language
  }

  fun getImplicitLanguages(projectId: Long): Set<Language> {
    val data = getPaged(
      projectId = projectId,
      PageRequest.of(0, 2, Sort.by(Sort.Order(Sort.Direction.ASC, "id")))
    )
    return data.content.toSet()
  }

  @Transactional
  fun findAll(projectId: Long): Set<Language> {
    return languageRepository.findAllByProjectId(projectId).toSet()
  }

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
    if (languages.size < tags.size) {
      throw NotFoundException(io.tolgee.constants.Message.LANGUAGE_NOT_FOUND)
    }
    val sortedByTagsParam = languages.sortedBy { language ->
      tags.indexOfFirst { tag -> language.tag == tag }
    }
    return sortedByTagsParam.toSet()
  }

  fun getLanguagesForTranslationsView(languages: Set<String>?, projectId: Long): Set<Language> {
    return if (languages == null) {
      getImplicitLanguages(projectId)
    } else findByTags(languages, projectId)
  }

  fun findByName(name: String?, project: Project): Optional<Language> {
    return languageRepository.findByNameAndProject(name, project)
  }

  fun deleteAllByProject(projectId: Long?) {
    languageRepository.deleteAllByProjectId(projectId)
  }

  fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
    return this.languageRepository.saveAll(languages)
  }

  fun getPaged(projectId: Long, pageable: Pageable): Page<Language> {
    return this.languageRepository.findAllByProjectId(projectId, pageable)
  }

  fun findByIdIn(ids: Iterable<Long>): List<Language> {
    return languageRepository.findAllById(ids)
  }
}
