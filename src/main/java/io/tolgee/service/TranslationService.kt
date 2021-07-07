package io.tolgee.service

import io.tolgee.constants.Message
import io.tolgee.dtos.PathDTO
import io.tolgee.dtos.query_results.KeyWithTranslationsDto
import io.tolgee.dtos.request.GetTranslationsParams
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto
import io.tolgee.dtos.response.KeyWithTranslationsResponseDto.Companion.fromQueryResult
import io.tolgee.dtos.response.ViewDataResponse
import io.tolgee.dtos.response.translations_view.ResponseParams
import io.tolgee.exceptions.InternalException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation.Companion.builder
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.query_builders.TranslationsViewBuilder
import io.tolgee.service.query_builders.TranslationsViewBuilderOld
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.query.AuditEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.stream.Collectors
import javax.persistence.EntityManager

@Service
@Transactional
class TranslationService(
        private val translationRepository: TranslationRepository,
        private val entityManager: EntityManager,
        private val importService: ImportService,
) {
    @Autowired
    private lateinit var languageService: LanguageService

    @Autowired
    private lateinit var keyService: KeyService

    @Autowired
    private lateinit var projectService: ProjectService

    @Transactional
    @Suppress("UNCHECKED_CAST")
    fun getTranslations(languageTags: Set<String>, projectId: Long): Map<String, Any> {
        val allByLanguages = translationRepository.getTranslations(languageTags, projectId)
        val langTranslations: HashMap<String, Any> = LinkedHashMap()
        for (translation in allByLanguages) {
            val map = langTranslations
                    .computeIfAbsent(translation.languageTag
                    ) { LinkedHashMap<String, Any>() } as MutableMap<String, Any?>
            addToMap(translation, map)
        }
        return langTranslations
    }

    fun getAllByLanguageId(languageId: Long): List<Translation> {
        return translationRepository.getAllByLanguageId(languageId)
    }

    fun getKeyTranslationsResult(projectId: Long, path: PathDTO?, languageTags: Set<String>?): Map<String, String?> {
        val project = projectService.get(projectId).orElseThrow { NotFoundException() }!!
        val key = keyService.get(project, path!!).orElse(null)
        val languages: Set<Language> = if (languageTags == null) {
            languageService.getImplicitLanguages(project)
        } else {
            languageService.findByTags(languageTags, projectId)
        }
        val translations = getKeyTranslations(languages, project, key)
        val translationsMap = translations.stream()
                .collect(Collectors.toMap({ v: Translation -> v.language!!.tag!! }, Translation::text))
        for (language in languages) {
            if (translationsMap.keys.stream().filter { l: String? -> l == language.tag }.findAny().isEmpty) {
                translationsMap[language.tag] = ""
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
    @Deprecated(replaceWith = ReplaceWith("getViewData"),
            message = "Use new getViewData. This is for old API compatibility.")
    fun getViewDataOld(
            languageTags: Set<String>?, projectId: Long?, limit: Int, offset: Int, search: String?
    ): ViewDataResponse<LinkedHashSet<KeyWithTranslationsResponseDto>, ResponseParams> {
        val project = projectService.get(projectId!!).orElseThrow { NotFoundException() }!!
        val languages: Set<Language> = languageService.getLanguagesForTranslationsView(languageTags, project)
        val (count, data1) = TranslationsViewBuilderOld.getData(entityManager, project, languages, search, limit, offset)
        return ViewDataResponse(data1
                .stream()
                .map { queryResult: Any? -> fromQueryResult(KeyWithTranslationsDto((queryResult as Array<Any?>))) }
                .collect(Collectors.toCollection { LinkedHashSet() }), offset, count,
                ResponseParams(search, languages.stream().map(Language::tag).collect(Collectors.toSet())))
    }

    @Suppress("UNCHECKED_CAST")
    fun getViewData(
            project: Project,
            pageable: Pageable,
            params: GetTranslationsParams,
            languages: Set<Language>
    ): Page<KeyWithTranslationsView> {
        return TranslationsViewBuilder.getData(entityManager, project, languages, pageable, params)
    }


    fun setTranslation(key: Key, languageTag: String?, text: String?): Translation {
        val language = languageService.findByTag(languageTag!!, key.project!!)
                .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        return setTranslation(key, language, text)
    }

    fun setTranslation(key: Key, language: Language, text: String?): Translation {
        val translation = getOrCreate(key, language)
        translation.text = text
        saveTranslation(translation)
        return translation
    }

    fun saveTranslation(translation: Translation): Translation {
        return translationRepository.save(translation)
    }

    fun setForKey(key: Key, translations: Map<String, String?>): Map<String, Translation> {
        return translations.entries.associate { (languageTag, value) ->
            if (value == null || value.isEmpty()) {
                deleteIfExists(key, languageTag)
                return@associate languageTag to null
            }
            languageTag to setTranslation(key, languageTag, value)
        }.filterValues { it != null }.mapValues { it.value as Translation }
    }

    fun deleteIfExists(key: Key, languageTag: String?) {
        val language = languageService.findByTag(languageTag!!, key.project!!)
                .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
        translationRepository.findOneByKeyAndLanguage(key, language)
                .ifPresent { entity: Translation -> translationRepository.delete(entity) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addToMap(translation: SimpleTranslationView, map: MutableMap<String, Any?>) {
        var currentMap = map
        val path = PathDTO.fromFullPath(translation.key)
        for (folderName in path.path) {
            val childMap = currentMap.computeIfAbsent(folderName) { LinkedHashMap<Any, Any>() }
            if (childMap is Map<*, *>) {
                currentMap = childMap as MutableMap<String, Any?>
                continue
            }
            throw InternalException(Message.DATA_CORRUPTED)
        }
        currentMap[path.name] = translation.text
    }

    private fun getHistory(key: Key, language: Language): MutableList<Any?>? {
        return AuditReaderFactory.get(entityManager)
                .createQuery()
                .forRevisionsOfEntity(Translation::class.java, true)
                .add(AuditEntity.and(
                        AuditEntity.property("language_id").eq(language.id),
                        AuditEntity.property("key_id").eq(key.id)
                )).resultList
    }

    fun deleteByIdIn(ids: Collection<Long>) {
        importService.onExistingTranslationsRemoved(ids)
        translationRepository.deleteByIdIn(ids)
    }

    fun deleteAllByProject(projectId: Long) {
        val ids = translationRepository.selectIdsByProject(projectId)
        deleteByIdIn(ids)
        entityManager.flush()
    }

    fun deleteAllByLanguage(languageId: Long) {
        val translations = translationRepository.getAllByLanguageId(languageId)
        val ids = translations.map { it.id }
        deleteByIdIn(ids)
    }

    fun deleteAllByKeys(ids: Collection<Long>) {
        val translations = translationRepository.getAllByKeyIdIn(ids)
        deleteByIdIn(translations.map { it.id })
    }

    fun deleteAllByKey(id: Long) {
        this.deleteAllByKeys(listOf(id))
    }

    fun saveAll(entities: Iterable<Translation?>) {
        translationRepository.saveAll(entities)
    }

    fun setState(translation: Translation, state: TranslationState): Translation {
        translation.state = state
        return this.saveTranslation(translation)
    }
}
