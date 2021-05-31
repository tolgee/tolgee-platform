package io.tolgee.service

import io.tolgee.collections.LanguageSet
import io.tolgee.constants.Message
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Language.Companion.fromRequestDTO
import io.tolgee.model.Repository
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
    fun createLanguage(dto: LanguageDTO?, repository: Repository): Language {
        val language = fromRequestDTO(dto!!)
        language.repository = repository
        repository.languages.add(language)
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

    fun getImplicitLanguages(repository: Repository): LanguageSet {
        return repository.languages.stream().limit(2).collect(Collectors.toCollection { LanguageSet() })
    }

    @Transactional
    fun findAll(repositoryId: Long?): LanguageSet {
        return LanguageSet(languageRepository.findAllByRepositoryId(repositoryId))
    }

    fun findById(id: Long): Optional<Language> {
        return languageRepository.findById(id)
    }

    fun findByAbbreviation(abbreviation: String, repository: Repository): Optional<Language> {
        return languageRepository.findByAbbreviationAndRepository(abbreviation, repository)
    }

    fun findByAbbreviation(abbreviation: String?, repositoryId: Long): Optional<Language> {
        return languageRepository.findByAbbreviationAndRepositoryId(abbreviation, repositoryId)
    }

    fun findByAbbreviations(abbreviations: Collection<String>?, repositoryId: Long?): LanguageSet {
        val langs = languageRepository.findAllByAbbreviationInAndRepositoryId(abbreviations, repositoryId)
        if (!langs.stream().map(Language::abbreviation).collect(Collectors.toSet()).containsAll(abbreviations!!)) {
            throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
        }
        return LanguageSet(langs)
    }

    @Transactional
    fun getOrCreate(repository: Repository, languageAbbreviation: String): Language {
        return this.findByAbbreviation(languageAbbreviation, repository)
                .orElseGet {
                    createLanguage(LanguageDTO(null, languageAbbreviation, languageAbbreviation), repository)
                }
    }

    fun getLanguagesForTranslationsView(languages: Set<String>?, repository: Repository): LanguageSet {
        return if (languages == null) {
            getImplicitLanguages(repository)
        } else findByAbbreviations(languages, repository.id)
    }

    fun findByName(name: String?, repository: Repository): Optional<Language> {
        return languageRepository.findByNameAndRepository(name, repository)
    }

    fun deleteAllByRepository(repositoryId: Long?) {
        languageRepository.deleteAllByRepositoryId(repositoryId)
    }

    @Autowired
    fun setTranslationService(translationService: TranslationService?) {
        this.translationService = translationService
    }

    fun saveAll(languages: Iterable<Language>): MutableList<Language>? {
        return this.languageRepository.saveAll(languages)
    }
}
