package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.query_results.KeyWithTranslationsDto
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto.Companion.fromQueryResult
import io.tolgee.dtos.response.ViewDataResponse
import io.tolgee.dtos.response.translations_view.ResponseParams
import io.tolgee.exceptions.InternalException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Translation
import io.tolgee.model.Translation.Companion.builder
import io.tolgee.model.key.Key
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.query_builders.TranslationsViewBuilder.Companion.getData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors
import javax.persistence.EntityManager

@Service
@Transactional
class TranslationService(private val translationRepository: TranslationRepository, private val entityManager: EntityManager) {
    private var languageService: LanguageService? = null
    private var keyService: KeyService? = null
    private var projectService: ProjectService? = null

    @Transactional
    @Suppress("UNCHECKED_CAST")
    fun getTranslations(languageAbbreviations: Set<String>, projectId: Long): Map<String, Any> {
        val allByLanguages = translationRepository.getTranslations(languageAbbreviations, projectId)
        val langTranslations: HashMap<String, Any> = LinkedHashMap()
        for (translation in allByLanguages) {
            val map = langTranslations
                    .computeIfAbsent(translation.language!!.abbreviation!!
                    ) { LinkedHashMap<String, Any>() } as MutableMap<String, Any?>
            addToMap(translation, map)
        }
        return langTranslations
    }

    fun getAllByLanguageId(languageId: Long): List<Translation> {
        return translationRepository.getAllByLanguageId(languageId)
    }

    fun getKeyTranslationsResult(projectId: Long, path: PathDTO?, languageAbbreviations: Set<String>?): Map<String, String?> {
        val project = projectService!!.get(projectId).orElseThrow { NotFoundException() }!!
        val key = keyService!!.get(project, path!!).orElse(null)
        val languages: Set<Language> = if (languageAbbreviations == null) {
            languageService!!.getImplicitLanguages(project)
        } else {
            languageService!!.findByAbbreviations(languageAbbreviations, projectId)
        }
        val translations = getKeyTranslations(languages, project, key)
        val translationsMap = translations.stream()
                .collect(Collectors.toMap({ v: Translation -> v.language!!.abbreviation!! }, Translation::text))
        for (language in languages) {
            if (translationsMap.keys.stream().filter { l: String? -> l == language.abbreviation }.findAny().isEmpty) {
                translationsMap[language.abbreviation] = ""
            }
        }
        return translationsMap
    }

    private fun getKeyTranslations(languages: Set<Language>, project: Project, key: Key?): Set<Translation> {
        return if (key != null) {
            translationRepository.getTranslations(key, project, languages)
        } else LinkedHashSet()
    }

    fun getOrCreate(key: Key, language: Language): Translation {
        return find(key, language).orElseGet { builder().language(language).key(key).build() }
    }

    fun find(key: Key, language: Language): Optional<Translation> {
        return translationRepository.findOneByKeyAndLanguage(key, language)
    }

    fun find(id: Long): Translation? {
        return this.translationRepository.findById(id).orElse(null)
    }

    @Suppress("UNCHECKED_CAST")
    fun getViewData(
            languageAbbreviations: Set<String>?, projectId: Long?, limit: Int, offset: Int, search: String?
    ): ViewDataResponse<LinkedHashSet<KeyWithTranslationsResponseDto>, ResponseParams> {
        val project = projectService!!.get(projectId!!).orElseThrow { NotFoundException() }!!
        val languages: Set<Language> = languageService!!.getLanguagesForTranslationsView(languageAbbreviations, project)
        val (count, data1) = getData(entityManager, project, languages, search, limit, offset)
        return ViewDataResponse(data1
                .stream()
                .map { queryResult: Any? -> fromQueryResult(KeyWithTranslationsDto((queryResult as Array<Any?>))) }
                .collect(Collectors.toCollection { LinkedHashSet() }), offset, count,
                ResponseParams(search, languages.stream().map(Language::abbreviation).collect(Collectors.toSet())))
    }

    fun setTranslation(key: Key, languageAbbreviation: String?, text: String?) {
        val language = languageService!!.findByAbbreviation(languageAbbreviation!!, key.project!!)
                .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        setTranslation(key, language, text)
    }

    fun setTranslation(key: Key, language: Language, text: String?) {
        val translation = getOrCreate(key, language)
        translation.text = text
        saveTranslation(translation)
    }

    fun saveTranslation(translation: Translation) {
        translationRepository.save(translation)
    }

    fun setForKey(key: Key, translations: Map<String, String?>) {
        for ((key1, value) in translations) {
            if (value == null || value.isEmpty()) {
                deleteIfExists(key, key1)
            }
            setTranslation(key, key1, value)
        }
    }

    fun deleteIfExists(key: Key, languageAbbreviation: String?) {
        val language = languageService!!.findByAbbreviation(languageAbbreviation!!, key.project!!)
                .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        translationRepository.findOneByKeyAndLanguage(key, language)
                .ifPresent { entity: Translation -> translationRepository.delete(entity) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addToMap(translation: Translation, map: MutableMap<String, Any?>) {
        var currentMap = map
        for (folderName in translation.key!!.path.path) {
            val childMap = currentMap.computeIfAbsent(folderName) { LinkedHashMap<Any, Any>() }
            if (childMap is Map<*, *>) {
                currentMap = childMap as MutableMap<String, Any?>
                continue
            }
            throw InternalException(Message.DATA_CORRUPTED)
        }
        currentMap[translation.key!!.path.name] = translation.text
    }

    fun deleteAllByRepository(projectId: Long) {
        translationRepository.deleteAllByProjectId(projectId)
    }

    fun deleteAllByLanguage(languageId: Long) {
        translationRepository.deleteAll(translationRepository.getAllByLanguageId(languageId))
    }

    fun deleteAllByKeys(ids: Collection<Long>) {
        translationRepository.deleteAll(translationRepository.getAllByKeyIdIn(ids))
    }

    fun deleteAllByKey(id: Long) {
        this.deleteAllByKeys(listOf(id))
    }

    @Autowired
    fun setLanguageService(languageService: LanguageService?) {
        this.languageService = languageService
    }

    @Autowired
    fun setKeyService(keyService: KeyService?) {
        this.keyService = keyService
    }

    @Autowired
    fun setRepositoryService(projectService: ProjectService?) {
        this.projectService = projectService
    }

    fun saveAll(entities: Iterable<Translation?>) {
        translationRepository.saveAll(entities)
    }
}
