package io.tolgee.service.translation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.events.OnTranslationsSet
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.helpers.TextHelper
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.query_builders.translationViewBuilder.TranslationViewDataProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import javax.persistence.EntityManager

@Service
@Transactional
class TranslationService(
  private val translationRepository: TranslationRepository,
  private val importService: ImportService,
  private val tolgeeProperties: TolgeeProperties,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val entityManager: EntityManager,
  private val translationCommentService: TranslationCommentService
) {
  @set:Autowired
  @set:Lazy
  lateinit var languageService: LanguageService

  @set:Autowired
  @set:Lazy
  lateinit var keyService: KeyService

  @set:Autowired
  @set:Lazy
  lateinit var projectService: ProjectService

  @Transactional
  @Suppress("UNCHECKED_CAST")
  fun getTranslations(
    languageTags: Set<String>,
    namespace: String?,
    projectId: Long,
    structureDelimiter: Char?
  ): Map<String, Any> {
    val safeNamespace = if (namespace == "") null else namespace
    val allByLanguages = translationRepository.getTranslations(languageTags, safeNamespace, projectId)
    val langTranslations: HashMap<String, Any> = LinkedHashMap()
    for (translation in allByLanguages) {
      val map = langTranslations
        .computeIfAbsent(
          translation.languageTag
        ) { LinkedHashMap<String, Any>() } as MutableMap<String, Any?>
      addToMap(translation, map, structureDelimiter)
    }
    return langTranslations
  }

  fun getAllByLanguageId(languageId: Long): List<Translation> {
    return translationRepository.getAllByLanguageId(languageId)
  }

  fun getKeyTranslations(languages: Set<Language>, project: Project, key: Key?): Set<Translation> {
    return if (key != null) {
      translationRepository.getTranslations(key, project, languages)
    } else LinkedHashSet()
  }

  fun getOrCreate(key: Key, language: Language): Translation {
    return find(key, language).orElseGet {
      Translation(language = language, key = key)
    }
  }

  fun getOrCreate(keyId: Long, languageId: Long): Translation {
    return translationRepository.findOneByKeyIdAndLanguageId(keyId, languageId)
      ?: let {
        val key = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
        val language = languageService.findById(languageId).orElseThrow { NotFoundException() }
        Translation().apply {
          this.key = key
          this.language = language
        }
      }
  }

  fun find(key: Key, language: Language): Optional<Translation> {
    return translationRepository.findOneByKeyAndLanguage(key, language)
  }

  fun get(id: Long): Translation {
    return this.find(id) ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
  }

  fun find(id: Long): Translation? {
    return this.translationRepository.findById(id).orElse(null)
  }

  fun getViewData(
    projectId: Long,
    pageable: Pageable,
    params: GetTranslationsParams,
    languages: Set<Language>
  ): Page<KeyWithTranslationsView> {
    return translationViewDataProvider.getData(projectId, languages, pageable, params, params.cursor)
  }

  fun getSelectAllKeys(
    projectId: Long,
    params: TranslationFilters,
    languages: Set<Language>
  ): List<Long> {
    return translationViewDataProvider.getSelectAllKeys(projectId, languages, params)
  }

  fun setTranslation(key: Key, languageTag: String?, text: String?): Translation? {
    val language = languageService.findByTag(languageTag!!, key.project)
      .orElseThrow { NotFoundException(Message.LANGUAGE_NOT_FOUND) }
    return setTranslation(key, language, text)
  }

  fun setTranslation(key: Key, language: Language, text: String?): Translation {
    val translation = getOrCreate(key, language)
    setTranslation(translation, text)
    key.translations.add(translation)
    return translation
  }

  fun setTranslation(
    translation: Translation,
    text: String?
  ): Translation {
    if (translation.text !== text) {
      translation.resetFlags()
    }
    translation.text = text
    if (translation.state == TranslationState.UNTRANSLATED && !translation.text.isNullOrEmpty()) {
      translation.state = TranslationState.TRANSLATED
    }
    if (text.isNullOrEmpty()) {
      translation.state = TranslationState.UNTRANSLATED
      translation.text = null
    }
    return save(translation)
  }

  fun save(translation: Translation): Translation {
    val translationTextLength = translation.text?.length ?: 0
    if (translationTextLength > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG, listOf(tolgeeProperties.maxTranslationTextLength))
    }
    return translationRepository.save(translation)
  }

  @Transactional
  fun setForKey(key: Key, translations: Map<String, String?>): Map<String, Translation> {
    val myKey = keyService.get(key.id)
    val languages = languageService.findByTags(translations.keys, key.project.id)
    val oldTranslations = getKeyTranslations(languages, key.project, key).associate { it.language.tag to it.text }

    return translations.entries.associate { (languageTag, value) ->
      languageTag to setTranslation(key, languageTag, value)
    }.filterValues { it != null }.mapValues { it.value as Translation }.also {
      applicationEventPublisher.publishEvent(
        OnTranslationsSet(
          source = this,
          key = myKey,
          oldValues = oldTranslations,
          translations = it.values.toList()
        )
      )
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun addToMap(translation: SimpleTranslationView, map: MutableMap<String, Any?>, delimiter: Char?) {
    var currentMap = map
    val path = TextHelper.splitOnNonEscapedDelimiter(translation.key, delimiter).toMutableList()
    val name = path.removeLast()
    for (folderName in path) {
      val childMap = currentMap.computeIfAbsent(folderName) { LinkedHashMap<Any, Any>() }
      if (childMap is Map<*, *>) {
        currentMap = childMap as MutableMap<String, Any?>
        continue
      }
      // there is already string value, so we cannot replace it by map,
      // we have to save the key directly without nesting
      map[translation.key] = translation.text
      return
    }
    // The result already contains the key, so we have to add it to root without nesting
    if (currentMap.containsKey(name)) {
      map[translation.key] = translation.text
      return
    }
    currentMap[name] = translation.text
  }

  fun deleteByIdIn(ids: Collection<Long>) {
    importService.onExistingTranslationsRemoved(ids)
    translationCommentService.deleteByTranslationIdIn(ids)
    translationRepository.deleteByIdIn(ids)
  }

  fun deleteAllByKeys(ids: Collection<Long>) {
    val translations = translationRepository.getAllByKeyIdIn(ids)
    val translationIds = translations.map { it.id }
    deleteByIdIn(translationIds)
  }

  fun deleteAllByKey(id: Long) {
    this.deleteAllByKeys(listOf(id))
  }

  fun saveAll(entities: Iterable<Translation>) {
    entities.map { save(it) }
  }

  fun setState(translation: Translation, state: TranslationState): Translation {
    translation.state = state
    translation.resetFlags()
    return this.save(translation)
  }

  fun findBaseTranslation(key: Key): Translation? {
    projectService.getOrCreateBaseLanguage(key.project.id)?.let {
      return find(key, it).orElse(null)
    }
    return null
  }

  fun getTranslationMemoryValue(
    key: Key,
    targetLanguage: Language,
  ): TranslationMemoryItemView? {
    val baseLanguage = projectService.getOrCreateBaseLanguage(targetLanguage.project.id)
      ?: throw NotFoundException(Message.BASE_LANGUAGE_NOT_FOUND)

    val baseTranslationText = findBaseTranslation(key)?.text ?: return null

    return translationRepository.getTranslationMemoryValue(
      baseTranslationText,
      key,
      baseLanguage,
      targetLanguage
    ).firstOrNull()
  }

  fun getTranslationMemorySuggestions(
    key: Key,
    targetLanguage: Language,
    pageable: Pageable
  ): Page<TranslationMemoryItemView> {
    val baseTranslation = findBaseTranslation(key) ?: return Page.empty()

    val baseTranslationText = baseTranslation.text ?: return Page.empty(pageable)

    return getTranslationMemorySuggestions(baseTranslationText, key, targetLanguage, pageable)
  }

  fun getTranslationMemorySuggestions(
    baseTranslationText: String,
    key: Key?,
    targetLanguage: Language,
    pageable: Pageable
  ): Page<TranslationMemoryItemView> {
    val baseLanguage = projectService.getOrCreateBaseLanguage(targetLanguage.project.id)
      ?: throw NotFoundException(Message.BASE_LANGUAGE_NOT_FOUND)

    return getTranslationMemorySuggestions(baseTranslationText, key, baseLanguage, targetLanguage, pageable)
  }

  fun getTranslationMemorySuggestions(
    sourceTranslationText: String,
    key: Key?,
    sourceLanguage: Language,
    targetLanguage: Language,
    pageable: Pageable
  ): Page<TranslationMemoryItemView> {
    if ((sourceTranslationText.length) < 3) {
      return Page.empty(pageable)
    }

    return translationRepository.getTranslateMemorySuggestions(
      baseTranslationText = sourceTranslationText,
      key = key,
      baseLanguage = sourceLanguage,
      targetLanguage = targetLanguage,
      pageable = pageable
    )
  }

  @Transactional
  fun dismissAutoTranslated(translation: Translation) {
    translation.auto = false
    translation.mtProvider = null
    save(translation)
  }

  @Transactional
  fun setOutdated(translation: Translation, value: Boolean) {
    translation.outdated = value
    save(translation)
  }

  fun setOutdated(key: Key, excludeTranslationIds: Set<Long> = emptySet()) {
    val baseLanguage = key.project.baseLanguage
    key.translations.forEach {
      val isBase = it.language.id == baseLanguage?.id
      val isEmpty = it.text.isNullOrEmpty()
      val isExcluded = excludeTranslationIds.contains(it.id)

      if (!isBase && !isEmpty && !isExcluded) {
        it.outdated = true
        it.state = TranslationState.TRANSLATED
        save(it)
      }
    }
  }

  fun setOutdatedBatch(keyIds: List<Long>) {
    translationRepository.setOutdated(keyIds)
  }

  fun get(keyLanguagesMap: Map<Key, List<Language>>): List<Translation> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Translation::class.java)
    val root = query.from(Translation::class.java)

    val predicates = keyLanguagesMap.map { (key, languages) ->
      cb.and(
        cb.equal(root.get(Translation_.key), key),
        root.get(Translation_.language).`in`(languages)
      )
    }.toTypedArray()

    query.where(cb.or(*predicates))

    return entityManager.createQuery(query).resultList
  }

  fun getForKeys(keyIds: List<Long>, languageTags: List<String>): List<Translation> {
    return translationRepository.getForKeys(keyIds, languageTags)
  }

  fun findAllByKeyIdsAndLanguageIds(
    keyIds: List<Long>,
    languageIds: List<Long>
  ): List<Translation> {
    return translationRepository.findAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)
  }

  @Transactional
  fun setState(keyIds: List<Long>, languageIds: List<Long>, state: TranslationState) {
    val translations = getTargetTranslations(keyIds, languageIds)
    translations.forEach { it.state = state }
    saveAll(translations)
  }

  fun getTranslations(
    keyIds: List<Long>,
    languageIds: List<Long>
  ) = translationRepository.getAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)

  fun clear(keyIds: List<Long>, languageIds: List<Long>) {
    val translations = getTargetTranslations(keyIds, languageIds)
    translations.forEach {
      it.state = TranslationState.UNTRANSLATED
      it.text = null
      it.outdated = false
      it.mtProvider = null
      it.auto = false
    }
    saveAll(translations)
  }

  fun copy(keyIds: List<Long>, sourceLanguageId: Long, targetLanguageIds: List<Long>) {
    val sourceTranslations = getTargetTranslations(keyIds, listOf(sourceLanguageId)).associateBy { it.key.id }
    val targetTranslations = getTargetTranslations(keyIds, targetLanguageIds).onEach {
      it.text = sourceTranslations[it.key.id]?.text
      if (!it.text.isNullOrEmpty()) {
        it.state = TranslationState.TRANSLATED
      }
      it.auto = false
      it.mtProvider = null
      it.outdated = false
    }
    saveAll(targetTranslations)
  }

  private fun getTargetTranslations(
    keyIds: List<Long>,
    targetLanguageIds: List<Long>
  ): List<Translation> {
    val existing = getTranslations(keyIds, targetLanguageIds)
    val existingMap = existing.groupBy { it.key.id }
      .map { entry ->
        entry.key to
          entry.value.associateBy { translation -> translation.language.id }
      }.toMap()
    return keyIds.flatMap { keyId ->
      targetLanguageIds.map { languageId ->
        existingMap[keyId]?.get(languageId) ?: getOrCreate(
          entityManager.getReference(Key::class.java, keyId),
          entityManager.getReference(Language::class.java, languageId)
        )
      }
    }
  }
}
