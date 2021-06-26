package io.tolgee.service

import io.tolgee.collections.LanguageSet
import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Project
import io.tolgee.repository.LanguageRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors
import javax.persistence.EntityManager

@Service
class LanguageService(
        private val languageRepository: LanguageRepository,
        private val entityManager: EntityManager,
) {
    private var translationService: TranslationService? = null

    @Transactional
    fun createLanguage(dto: LanguageDto?, project: Project): Language {
        val language = fromRequestDTO(dto!!)
        language.project = project
        project.languages.add(language)
        languageRepository.save(language)
        return language
    }

    @Transactional
    fun deleteLanguage(id: Long) {
        val language = languageRepository.findById(id).orElseThrow { NotFoundException() }
        translationService!!.deleteAllByLanguage(language.id)
        languageRepository.delete(language)
    }

    @Transactional
    fun editLanguage(id: Long, dto: LanguageDto): Language {
        val language = languageRepository.findById(id).orElseThrow { NotFoundException() }
        language.updateByDTO(dto)
        entityManager.persist(language)
        return language
    }

    fun getImplicitLanguages(project: Project): LanguageSet {
        val data = getPaged(projectId = project.id, PageRequest.of(0, 2))
        return data.content.toCollection(LanguageSet())
    }

    @Transactional
    fun findAll(projectId: Long?): LanguageSet {
        return LanguageSet(languageRepository.findAllByProjectId(projectId))
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

    fun findByTags(tag: Collection<String>?, projectId: Long?): LanguageSet {
        val langs = languageRepository.findAllByTagInAndProjectId(tag, projectId)
        if (!langs.stream().map(Language::tag).collect(Collectors.toSet()).containsAll(tag!!)) {
            throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
        }
        return LanguageSet(langs)
    }

    fun getLanguagesForTranslationsView(languages: Set<String>?, project: Project): LanguageSet {
        return if (languages == null) {
            getImplicitLanguages(project)
        } else findByTags(languages, project.id)
    }

    fun findByName(name: String?, project: Project): Optional<Language> {
        return languageRepository.findByNameAndProject(name, project)
    }

    fun deleteAllByProject(projectId: Long?) {
        languageRepository.deleteAllByProjectId(projectId)
    }

    @Autowired
    fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

    fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
        return this.languageRepository.saveAll(languages)
    }

    fun getPaged(projectId: Long, pageable: Pageable): Page<Language> {
        return this.languageRepository.findAllByProjectId(projectId, pageable)
    }
}
