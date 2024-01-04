package io.tolgee.service.translation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.KeyAndLanguage
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.repository.TranslationRepository
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.KeyService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class TranslationService(
  private val translationRepository: TranslationRepository,
  private val importService: ImportService,
  private val tolgeeProperties: TolgeeProperties,
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val entityManager: EntityManager,
  @Lazy
  private val translationCommentService: TranslationCommentService,
  private val translationUpdateHelper: TranslationUpdateHelper,
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

  fun getAllByLanguageId(languageId: Long): List<Translation> {
    return translationRepository.getAllByLanguageId(languageId)
  }

  fun getKeyTranslations(
    languages: Set<Language>,
    project: Project,
    key: Key?,
  ): Set<Translation> {
    return if (key != null) {
      translationRepository.getTranslations(key, project, languages)
    } else {
      LinkedHashSet()
    }
  }

  fun getOrCreate(
    key: Key,
    language: Language,
    projectId: Long,
  ): Translation {
    return getOrCreate(listOf(KeyAndLanguage(key, language)), projectId).values.single()
  }

  fun getOrCreate(
    items: Collection<KeyAndLanguage>,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    val existing = translationUpdateHelper.getExistingTranslations(items, projectId)

    return items.associateWith { item ->
      existing[item] ?: create(item.key, item.language)
    }
  }

  private fun create(
    key: Key,
    language: Language,
  ): Translation {
    return Translation(language = language, key = key)
  }

  fun getOrCreate(
    keyId: Long,
    languageId: Long,
    projectId: Long,
  ): Translation {
    val key = keyService.get(keyId, projectId)
    val language = languageService.get(languageId, projectId)

    return getOrCreate(
      listOf(
        KeyAndLanguage(
          key,
          language,
        ),
      ),
      projectId,
    ).values.single()
  }

  fun find(
    key: Key,
    language: Language,
  ): Optional<Translation> {
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
    languages: Set<Language>,
  ): Page<KeyWithTranslationsView> {
    return translationViewDataProvider.getData(projectId, languages, pageable, params, params.cursor)
  }

  fun getSelectAllKeys(
    projectId: Long,
    params: TranslationFilters,
    languages: Set<Language>,
  ): List<Long> {
    return translationViewDataProvider.getSelectAllKeys(projectId, languages, params)
  }

  fun save(translation: Translation): Translation {
    val translationTextLength = translation.text?.length ?: 0
    if (translationTextLength > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG, listOf(tolgeeProperties.maxTranslationTextLength))
    }
    return translationRepository.save(translation)
  }

  @Transactional
  fun set(
    key: Key,
    language: Language,
    text: String,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    return set(mapOf(KeyAndLanguage(key, language) to text), projectId)
  }

  @Transactional
  fun set(
    key: Key,
    translations: Map<String, String?>,
    projectId: Long,
  ): Map<String, Translation> {
    val languages = languageService.findByTags(translations.keys, projectId)
    val oldTranslations =
      getKeyTranslations(languages, entityManager.getReference(Project::class.java, projectId), key).associate {
        languages.byId(
          it.language.id,
        ) to it.text
      }

    return set(
      key = key,
      translations =
        translations.map { languages.byTag(it.key) to it.value }
          .toMap(),
      oldTranslations = oldTranslations,
      projectId = projectId,
    ).mapKeys { it.key.tag }
  }

  @Transactional
  fun set(
    key: Key,
    translations: Map<Language, String?>,
    oldTranslations: Map<Language, String?>,
    projectId: Long,
  ): Map<Language, Translation> {
    val newTranslations =
      translations.mapKeys { KeyAndLanguage(key, it.key) }
    val oldTranslationMap = oldTranslations.mapKeys { KeyAndLanguage(key, it.key) }
    return setWithOld(newTranslations, oldTranslationMap, projectId).mapKeys { it.key.language }
  }

  @Transactional
  fun set(
    translations: Map<KeyAndLanguage, String?>,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    val existingTranslations =
      translationUpdateHelper
        .getExistingTranslations(translations.keys, projectId)

    return set(translations, existingTranslations, projectId)
  }

  fun set(
    translations: Map<KeyAndLanguage, String?>,
    existingTranslations: Map<KeyAndLanguage, Translation?>,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    val oldTranslations =
      existingTranslations
        .mapKeys { it.key }.mapValues { it.value?.text }

    val translationEntities = getPreparedTranslations(existingTranslations, translations.keys)

    return set(translations, oldTranslations, projectId, translationEntities)
  }

  private fun setWithOld(
    newTranslations: Map<KeyAndLanguage, String?>,
    oldTranslations: Map<KeyAndLanguage, String?>,
    projectId: Long,
  ): Map<KeyAndLanguage, Translation> {
    val translationEntities = getOrCreate(newTranslations.keys, projectId)
    return set(newTranslations, oldTranslations, projectId, translationEntities)
  }

  /**
   * @param translationEntities map of new or existing translations prepared for text update
   */
  private fun set(
    newTranslations: Map<KeyAndLanguage, String?>,
    oldTranslations: Map<KeyAndLanguage, String?>,
    projectId: Long,
    translationEntities: Map<KeyAndLanguage, Translation>,
  ): Map<KeyAndLanguage, Translation> {
    translationEntities.forEach { (key, language), translation ->
      val newText = newTranslations[KeyAndLanguage(key, language)]
      if (translation.text != newText) {
        translation.resetFlags()
      }
      translation.text = newText

      save(translation)

      key.translations.add(translation)
    }
    handleOutdatedState(translationEntities, oldTranslations, projectId)
    return translationEntities
  }

  fun getPreparedTranslations(
    existingTranslations: Map<KeyAndLanguage, Translation?>,
    requiredKeysAndLanguages: Set<KeyAndLanguage>,
  ): Map<KeyAndLanguage, Translation> {
    return requiredKeysAndLanguages.associate {
      KeyAndLanguage(it.key, it.language) to (
        existingTranslations[it]
          ?: create(it.key, it.language)
      )
    }
  }

  fun handleOutdatedState(
    newTranslations: Map<KeyAndLanguage, Translation>,
    oldTranslations: Map<KeyAndLanguage, String?>,
    projectId: Long,
  ) {
    val baseLanguage = projectService.get(projectId).baseLanguage ?: return

    // we don't want to set outdated flag for just modified translations
    val excluded = newTranslations.values.map { it.id }
    val keys =
      newTranslations.map { it.key.key }.toSet().filter { key ->
        val oldBaseValue = oldTranslations[KeyAndLanguage(key, baseLanguage)]
        val newBaseValue = newTranslations[KeyAndLanguage(key, baseLanguage)]?.text
        oldBaseValue != newBaseValue
      }

    this.setOutdated(keys.map { it.id }, excluded, baseLanguage.id)
  }

  fun findForKeyByLanguages(
    key: Key,
    languageTags: Collection<String>,
  ): List<Translation> {
    return translationRepository.findForKeyByLanguages(key, languageTags)
  }

  private fun Collection<Language>.byTag(tag: String) =
    find { it.tag == tag } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

  private fun Collection<Language>.byId(id: Long) =
    find { it.id == id } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)

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
    entities.forEach { save(it) }
  }

  fun setStateBatch(
    translation: Translation,
    state: TranslationState,
  ): Translation {
    translation.state = state
    translation.resetFlags()
    return this.save(translation)
  }

  fun setStateBatch(states: Map<Translation, TranslationState>) {
    states.forEach { (translation, newState) ->
      translation.state = newState
      translation.resetFlags()
      this.save(translation)
    }
  }

  @Transactional
  fun dismissAutoTranslated(translation: Translation) {
    translation.auto = false
    translation.mtProvider = null
    save(translation)
  }

  @Transactional
  fun setOutdated(
    translation: Translation,
    value: Boolean,
  ) {
    translation.outdated = value
    save(translation)
  }

  fun setOutdated(
    keyIds: Collection<Long>,
    excludeTranslationIds: Collection<Long>,
    baseLanguageId: Long,
  ) {
    val translations =
      translationRepository
        .getTranslationsToSetOutDated(keyIds, excludeTranslationIds, baseLanguageId)

    translations.forEach {
      it.outdated = true
      it.state = TranslationState.TRANSLATED
    }
  }

  fun setOutdatedBatch(keyIds: List<Long>) {
    translationRepository.setOutdated(keyIds)
  }

  fun get(keyLanguagesMap: Map<Key, List<Language>>): List<Translation> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Translation::class.java)
    val root = query.from(Translation::class.java)

    val predicates =
      keyLanguagesMap.map { (key, languages) ->
        cb.and(
          cb.equal(root.get(Translation_.key), key),
          root.get(Translation_.language).`in`(languages),
        )
      }.toTypedArray()

    query.where(cb.or(*predicates))

    return entityManager.createQuery(query).resultList
  }

  fun getForKeys(
    keyIds: List<Long>,
    languageTags: List<String>,
  ): List<Translation> {
    return translationRepository.getForKeys(keyIds, languageTags)
  }

  fun findAllByKeyIdsAndLanguageIds(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): List<Translation> {
    return translationRepository.findAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)
  }

  @Transactional
  fun setStateBatch(
    keyIds: List<Long>,
    languageIds: List<Long>,
    state: TranslationState,
  ) {
    val translations = getTargetTranslationsForBatch(keyIds, languageIds)
    translations.filter { it.state != TranslationState.DISABLED }.forEach { it.state = state }
    saveAll(translations)
  }

  fun getTranslations(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ) = translationRepository.getAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)

  fun clearBatch(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ) {
    val translations = getTargetTranslationsForBatch(keyIds, languageIds)
    translations.forEach {
      it.clear()
    }
    saveAll(translations)
  }

  fun copyBatch(
    keyIds: List<Long>,
    sourceLanguageId: Long,
    targetLanguageIds: List<Long>,
  ) {
    val sourceTranslations = getTargetTranslationsForBatch(keyIds, listOf(sourceLanguageId)).associateBy { it.key.id }
    val targetTranslations =
      getTargetTranslationsForBatch(keyIds, targetLanguageIds).onEach {
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

  private fun getTargetTranslationsForBatch(
    keyIds: List<Long>,
    targetLanguageIds: List<Long>,
  ): List<Translation> {
    val keyAndLanguages =
      keyIds.flatMap { keyId ->
        targetLanguageIds.map { languageId ->
          KeyAndLanguage(
            entityManager.getReference(Key::class.java, keyId),
            entityManager.getReference(Language::class.java, languageId),
          )
        }
      }

    return getOrCreate(keyAndLanguages, keyAndLanguages.first().key.project.id).values
      .filter { it.state !== TranslationState.DISABLED }
  }

  fun deleteAllByProject(projectId: Long) {
    translationCommentService.deleteAllByProject(projectId)
    entityManager.createNativeQuery(
      "DELETE FROM translation " +
        "WHERE " +
        "key_id IN (SELECT id FROM key WHERE project_id = :projectId) or " +
        "language_id IN (SELECT id FROM language WHERE project_id = :projectId)",
    ).setParameter("projectId", projectId).executeUpdate()
  }

  fun get(
    translationId: Long,
    projectId: Long,
  ): Translation {
    return translationRepository.find(translationId, projectId)
      ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
  }
}
