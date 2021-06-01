package io.tolgee.service

import io.tolgee.collections.LanguageSet
import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Project
import io.tolgee.repository.LanguageRepository
import org.springframework.beans.factory.annotation.Autowired
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
    fun createLanguage(dto: LanguageDTO?, project: Project): Language {
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
    fun editLanguage(dto: LanguageDTO): Language {
        val language = languageRepository.findById(dto.id!!).orElseThrow { NotFoundException() }
        language.updateByDTO(dto)
        entityManager.persist(language)
        return language
    }

    fun getImplicitLanguages(project: Project): LanguageSet {
        return project.languages.stream().limit(2).collect(Collectors.toCollection { LanguageSet() })
    }

    @Transactional
    fun findAll(projectId: Long?): LanguageSet {
        return LanguageSet(languageRepository.findAllByProjectId(projectId))
    }

    fun findById(id: Long): Optional<Language> {
        return languageRepository.findById(id)
    }

    fun findByAbbreviation(abbreviation: String, project: Project): Optional<Language> {
        return languageRepository.findByAbbreviationAndProject(abbreviation, project)
    }

    fun findByAbbreviation(abbreviation: String?, projectId: Long): Optional<Language> {
        return languageRepository.findByAbbreviationAndProjectId(abbreviation, projectId)
    }

    fun findByAbbreviations(abbreviations: Collection<String>?, projectId: Long?): LanguageSet {
        val langs = languageRepository.findAllByAbbreviationInAndProjectId(abbreviations, projectId)
        if (!langs.stream().map(Language::abbreviation).collect(Collectors.toSet()).containsAll(abbreviations!!)) {
            throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
        }
        return LanguageSet(langs)
    }

    @Transactional
    fun getOrCreate(project: Project, languageAbbreviation: String): Language {
        return this.findByAbbreviation(languageAbbreviation, project)
                .orElseGet {
                    createLanguage(LanguageDTO(null, languageAbbreviation, languageAbbreviation), project)
                }
    }

    fun getLanguagesForTranslationsView(languages: Set<String>?, project: Project): LanguageSet {
        return if (languages == null) {
            getImplicitLanguages(project)
        } else findByAbbreviations(languages, project.id)
    }

    fun findByName(name: String?, project: Project): Optional<Language> {
        return languageRepository.findByNameAndProject(name, project)
    }

    fun deleteAllByRepository(projectId: Long?) {
        languageRepository.deleteAllByProjectId(projectId)
    }

    @Autowired
    fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

    fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
        return this.languageRepository.saveAll(languages)
    }
}
