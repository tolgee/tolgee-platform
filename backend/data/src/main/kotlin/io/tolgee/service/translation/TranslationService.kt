package io.tolgee.service.translation

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.queryResults.qa.KeyLanguagePairView
import io.tolgee.dtos.request.translation.GetTranslationsParams
import io.tolgee.dtos.request.translation.TranslationFilters
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.formats.PluralForms
import io.tolgee.formats.StringIsNotPluralException
import io.tolgee.formats.convertToIcuPlural
import io.tolgee.formats.getPluralForms
import io.tolgee.formats.normalizePlurals
import io.tolgee.formats.optimizePluralForms
import io.tolgee.helpers.TextHelper
import io.tolgee.model.ILanguage
import io.tolgee.model.Language
import io.tolgee.model.Language_
import io.tolgee.model.Project
import io.tolgee.model.Project_
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.Key_
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.Translation_
import io.tolgee.model.views.KeyWithTranslationsView
import io.tolgee.model.views.SimpleTranslationView
import io.tolgee.model.views.TranslationMemoryItemView
import io.tolgee.repository.TranslationRepository
import io.tolgee.security.ProjectHolder
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.key.KeyService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.qa.TranslationQaIssueService
import io.tolgee.service.queryBuilders.translationViewBuilder.TranslationViewDataProvider
import io.tolgee.service.translation.SetTranslationTextUtil.Companion.Options
import io.tolgee.util.nullIfEmpty
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.util.Optional

@Service
@Transactional
class TranslationService(
  private val translationRepository: TranslationRepository,
  private val importService: ImportService,
  private val tolgeeProperties: TolgeeProperties,
  private val applicationContext: ApplicationContext,
  private val translationViewDataProvider: TranslationViewDataProvider,
  private val entityManager: EntityManager,
  private val translationCommentService: TranslationCommentService,
  private val translationQaIssueService: TranslationQaIssueService,
) {
  @Autowired
  private lateinit var projectHolder: ProjectHolder

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
    branch: String?,
    projectId: Long,
    structureDelimiter: Char?,
    filterTag: List<String>? = null,
  ): Map<String, Any> {
    val safeNamespace = if (namespace == "") null else namespace
    val allByLanguages =
      translationRepository.getTranslations(languageTags, safeNamespace, branch, projectId, filterTag)
    val langTranslations: HashMap<String, Any> = LinkedHashMap()
    for (translation in allByLanguages) {
      val map =
        langTranslations
          .computeIfAbsent(
            translation.languageTag,
          ) { LinkedHashMap<String, Any>() } as MutableMap<String, Any?>
      addToMap(translation, map, structureDelimiter)
    }
    return langTranslations
  }

  fun getAllByLanguageId(
    languageId: Long,
    branch: String? = null,
  ): List<Translation> {
    return translationRepository.getAllByLanguageId(languageId, branch)
  }

  fun getKeyTranslations(
    languages: Set<ILanguage>,
    project: Project,
    key: Key?,
  ): Set<Translation> {
    return if (key != null) {
      translationRepository.getTranslations(key, project, languages.map { it.id })
    } else {
      LinkedHashSet()
    }
  }

  fun getOrCreate(
    key: Key,
    language: Language,
  ): Translation {
    return find(key, language).orElseGet {
      Translation(language = language, key = key)
    }
  }

  fun getOrCreate(
    projectId: Long,
    keyId: Long,
    languageId: Long,
  ): Translation {
    return translationRepository.findOneByProjectIdAndKeyIdAndLanguageId(projectId, keyId, languageId)
      ?: this.createEmpty(keyId, languageId, projectId)
  }

  fun createEmpty(
    keyId: Long,
    languageId: Long,
    projectId: Long? = null,
  ): Translation {
    val key = keyService.findOptional(keyId).orElseThrow { NotFoundException() }
    if (projectId != null && key.project.id != projectId) {
      throw BadRequestException(Message.KEY_NOT_FROM_PROJECT)
    }
    val language = languageService.getEntity(languageId)
    if (key.project.id != language.project.id) {
      throw BadRequestException(Message.LANGUAGE_NOT_FROM_PROJECT)
    }
    return Translation().apply {
      this.key = key
      this.language = language
    }
  }

  fun find(
    key: Key,
    language: ILanguage,
  ): Optional<Translation> {
    return translationRepository.findOneByKeyAndLanguageId(key, language.id)
  }

  fun get(id: Long): Translation {
    return this.find(id) ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
  }

  fun find(id: Long): Translation? {
    return this.translationRepository.findById(id).orElse(null)
  }

  fun find(
    projectId: Long,
    translationId: Long,
  ): Translation? {
    return this.translationRepository.find(projectId, translationId)
  }

  fun get(
    projectId: Long,
    translationId: Long,
  ): Translation {
    return find(projectId, translationId) ?: throw NotFoundException(Message.TRANSLATION_NOT_FOUND)
  }

  fun getViewData(
    projectId: Long,
    pageable: Pageable,
    params: GetTranslationsParams,
    languages: Set<LanguageDto>,
  ): Page<KeyWithTranslationsView> {
    return translationViewDataProvider.getData(
      projectId,
      languages,
      pageable,
      params,
      params.cursor,
      includeQaIssues = params.includeQaIssues == true,
    )
  }

  fun getSelectAllKeys(
    projectId: Long,
    params: TranslationFilters,
    languages: Set<LanguageDto>,
  ): List<Long> {
    return translationViewDataProvider.getSelectAllKeys(projectId, languages, params)
  }

  @Transactional
  fun setForKey(
    key: Key,
    translations: Map<String, String?>,
    options: Options? = null,
  ): Map<String, Translation> {
    return SetTranslationTextUtil(applicationContext).setForKey(key, translations, options)
  }

  @Transactional
  fun setForKey(
    key: Key,
    translations: Map<Language, String?>,
    oldTranslations: Map<Language, String?>,
  ): Map<Language, Translation> {
    return SetTranslationTextUtil(
      applicationContext,
    ).setForKey(key, translations, oldTranslations)
  }

  fun setTranslationText(
    key: Key,
    language: Language,
    text: String?,
  ): Translation {
    return SetTranslationTextUtil(
      applicationContext,
    ).setTranslationText(key, language, text)
  }

  fun setTranslationText(
    translation: Translation,
    text: String?,
  ): Translation {
    return SetTranslationTextUtil(applicationContext).setTranslationText(translation, text)
  }

  fun setTranslationTextNoSave(
    translation: Translation,
    text: String?,
  ) {
    return SetTranslationTextUtil(
      applicationContext,
    ).setTranslationTextNoSave(translation, text)
  }

  fun save(translation: Translation): Translation {
    val translationTextLength = translation.text?.length ?: 0
    if (translationTextLength > tolgeeProperties.maxTranslationTextLength) {
      throw BadRequestException(Message.TRANSLATION_TEXT_TOO_LONG, listOf(tolgeeProperties.maxTranslationTextLength))
    }
    return translationRepository.save(translation)
  }

  fun findForKeyByLanguages(
    key: Key,
    languageTags: Collection<String>,
  ): List<Translation> {
    return translationRepository.findForKeyByLanguages(key, languageTags)
  }

  @Suppress("UNCHECKED_CAST")
  private fun addToMap(
    translation: SimpleTranslationView,
    map: MutableMap<String, Any?>,
    delimiter: Char?,
  ) {
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

  fun findBaseTranslation(key: Key): Translation? {
    val baseLanguage = projectService.getOrAssignBaseLanguage(key.project.id)
    return find(key, baseLanguage).orElse(null)
  }

  fun getTranslationMemoryValue(
    key: Key,
    targetLanguage: ILanguage,
  ): TranslationMemoryItemView? {
    val baseTranslationText = findBaseTranslation(key)?.text ?: return null

    return translationRepository
      .getTranslationMemoryValue(
        baseTranslationText,
        key,
        targetLanguage.id,
      ).firstOrNull()
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
    key: Key,
    excludeTranslationIds: Set<Long> = emptySet(),
  ) {
    val baseLanguage = key.project.baseLanguage
    key.translations.forEach {
      val isBase = it.language.id == baseLanguage?.id
      val isEmpty = it.text.isNullOrEmpty()
      val isExcluded = excludeTranslationIds.contains(it.id)

      if (!isBase && !isEmpty && !isExcluded) {
        val project = projectHolder.projectOrNull
        it.outdated = true
        if (project?.translationProtection != TranslationProtection.PROTECT_REVIEWED) {
          it.state = TranslationState.TRANSLATED
        }
        save(it)
      }
    }
  }

  fun setOutdatedBatch(keyIds: List<Long>) {
    translationRepository.setOutdated(keyIds)
  }

  fun get(keyLanguagesMap: Map<Key, List<LanguageDto>>): List<Translation> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Translation::class.java)
    val root = query.from(Translation::class.java)

    val predicates =
      keyLanguagesMap
        .map { (key, languages) ->
          cb.and(
            cb.equal(root.get(Translation_.key), key),
            root.get(Translation_.language).get(Language_.id).`in`(languages.map { it.id }),
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

  fun <T> validateAndNormalizePlurals(
    texts: Map<T, String?>,
    isKeyPlural: Boolean,
    pluralArgName: String?,
  ): Map<T, String?> {
    if (isKeyPlural) {
      return validateAndNormalizePlurals(texts, pluralArgName)
    }

    return texts
  }

  fun <T> validateAndNormalizePlurals(
    texts: Map<T, String?>,
    pluralArgName: String?,
  ): Map<T, String?> {
    @Suppress("UNCHECKED_CAST")
    return try {
      normalizePlurals(texts, pluralArgName)
    } catch (e: StringIsNotPluralException) {
      throw BadRequestException(Message.INVALID_PLURAL_FORM, listOf(e.invalidStrings) as List<Serializable?>)
    }
  }

  @Transactional
  fun setStateBatch(
    keyIds: List<Long>,
    languageIds: List<Long>,
    state: TranslationState,
  ) {
    val translations = getTargetTranslations(keyIds, languageIds)
    translations.filter { it.state != TranslationState.DISABLED }.forEach { it.state = state }
    saveAll(translations)
  }

  fun getTranslations(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ) = translationRepository.getAllByKeyIdInAndLanguageIdIn(keyIds, languageIds)

  fun getAllByKeyId(keyId: Long): List<Translation> = translationRepository.getAllByKeyIdIn(listOf(keyId)).toList()

  fun clearBatch(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ) {
    val translations = getTargetTranslations(keyIds, languageIds)
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
    val sourceTranslations = getTargetTranslations(keyIds, listOf(sourceLanguageId)).associateBy { it.key.id }
    val targetTranslations =
      getTargetTranslations(keyIds, targetLanguageIds).onEach {
        it.text = sourceTranslations[it.key.id]?.text
        it.qaChecksStale = true
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
    targetLanguageIds: List<Long>,
  ): List<Translation> {
    val existing = getTranslations(keyIds, targetLanguageIds)
    val existingMap =
      existing
        .groupBy { it.key.id }
        .map { entry ->
          entry.key to
            entry.value.associateBy { translation -> translation.language.id }
        }.toMap()
    return keyIds.flatMap { keyId ->
      targetLanguageIds
        .map { languageId ->
          existingMap[keyId]?.get(languageId) ?: getOrCreate(
            entityManager.getReference(Key::class.java, keyId),
            entityManager.getReference(Language::class.java, languageId),
          )
        }.filter { it.state !== TranslationState.DISABLED }
    }
  }

  fun deleteAllByProject(projectId: Long) {
    translationCommentService.deleteAllByProject(projectId)
    translationQaIssueService.deleteAllByProjectId(projectId)
    entityManager
      .createNativeQuery(
        "DELETE FROM translation " +
          "WHERE " +
          "key_id IN (SELECT id FROM key WHERE project_id = :projectId) or " +
          "language_id IN (SELECT id FROM language WHERE project_id = :projectId)",
      ).setParameter("projectId", projectId)
      .executeUpdate()
  }

  fun onKeyMaxCharLimitChanged(keyId: Long) {
    val translations = translationRepository.getAllByKeyIdIn(listOf(keyId))
    if (translations.isEmpty()) return
    translations.forEach { it.qaChecksStale = true }
    saveAll(translations)
  }

  fun onKeyIsPluralChanged(
    keyIdToArgNameMap: Map<Long, String?>,
    newIsPlural: Boolean,
    ignoreTranslationsForMigration: MutableList<Long> = mutableListOf(),
    throwOnDataLoss: Boolean = false,
  ) {
    val translations =
      translationRepository
        .getAllByKeyIdInExcluding(keyIdToArgNameMap.keys, ignoreTranslationsForMigration.nullIfEmpty())
    translations.forEach { handleIsPluralChanged(it, newIsPlural, keyIdToArgNameMap[it.key.id], throwOnDataLoss) }
    saveAll(translations)
  }

  private fun handleIsPluralChanged(
    it: Translation,
    newIsPlural: Boolean,
    newPluralArgName: String?,
    throwOnDataLoss: Boolean,
  ) {
    it.text = getNewText(it.text, newIsPlural, newPluralArgName, throwOnDataLoss)
    it.qaChecksStale = true
  }

  /**
   * @param newIsPlural - if true, we are converting value to plural,
   * if not, we are converting it from plural
   */
  private fun getNewText(
    text: String?,
    newIsPlural: Boolean,
    newPluralArgName: String?,
    throwOnDataLoss: Boolean,
  ): String? {
    if (newIsPlural) {
      return text.convertToIcuPlural(newPluralArgName)
    }
    val forms = getPluralForms(text)
    if (throwOnDataLoss) {
      throwOnDataLoss(forms, text)
    }
    return forms?.forms?.get("other") ?: text
  }

  private fun throwOnDataLoss(
    forms: PluralForms?,
    text: String? = null,
  ) {
    forms ?: return
    val keys = optimizePluralForms(forms.forms).keys
    if (keys.size > 1) {
      throw BadRequestException(Message.PLURAL_FORMS_DATA_LOSS, listOf(text))
    }
  }

  @Transactional
  fun getTranslationsWithLabels(
    keyIds: List<Long>,
    languageIds: List<Long>,
  ): List<Translation> {
    return translationRepository
      .getTranslationsWithLabels(keyIds, languageIds)
  }

  fun getTranslationIdsByKeyIds(
    keyIds: List<Long>,
    languageIds: List<Long>? = null,
  ): List<Long> {
    if (keyIds.isEmpty()) return emptyList()
    return keyIds.chunked(1000).flatMap { chunk ->
      val cb = entityManager.criteriaBuilder
      val query = cb.createQuery(Long::class.java)
      val root = query.from(Translation::class.java)

      val predicates =
        mutableListOf<Predicate>(
          root.get(Translation_.key).get<Long>("id").`in`(chunk),
        )

      if (!languageIds.isNullOrEmpty()) {
        predicates.add(root.get(Translation_.language).get<Long>("id").`in`(languageIds))
      }

      query.select(root.get(Translation_.id))
      query.where(*predicates.toTypedArray())

      entityManager.createQuery(query).resultList
    }
  }

  fun getNotStaleTranslationIds(
    projectId: Long,
    languageIds: List<Long>? = null,
  ): List<Long> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(Long::class.java)
    val root = query.from(Translation::class.java)

    val predicates =
      mutableListOf<Predicate>(
        cb.equal(root.get(Translation_.key).get<Any>("project").get<Long>("id"), projectId),
      )

    if (!languageIds.isNullOrEmpty()) {
      predicates.add(root.get(Translation_.language).get<Long>("id").`in`(languageIds))
    }

    predicates.add(cb.equal(root.get(Translation_.qaChecksStale), false))

    query.select(root.get(Translation_.id))
    query.where(*predicates.toTypedArray())

    return entityManager.createQuery(query).resultList
  }

  @Transactional
  fun setQaChecksStale(translationIds: List<Long>) {
    translationIds.chunked(1000).forEach { chunk ->
      entityManager
        .createQuery(
          "UPDATE Translation t SET t.qaChecksStale = true WHERE t.id IN :ids",
        ).setParameter("ids", chunk)
        .executeUpdate()
    }
  }

  fun getKeyLanguagePairsForRecheck(
    projectId: Long,
    languageIds: List<Long>? = null,
    onlyStale: Boolean = false,
  ): List<KeyLanguagePairView> {
    val cb = entityManager.criteriaBuilder
    val query = cb.createTupleQuery()

    val keyRoot = query.from(Key::class.java)
    val projectJoin = keyRoot.join(Key_.project)
    val langJoin = projectJoin.join(Project_.languages)

    val translationJoin =
      keyRoot.join(Key_.translations, JoinType.LEFT)
    translationJoin.on(
      cb.equal(translationJoin.get(Translation_.language), langJoin),
    )

    val predicates =
      mutableListOf(
        cb.equal(projectJoin.get<Long>("id"), projectId),
      )

    if (!languageIds.isNullOrEmpty()) {
      predicates.add(langJoin.get(Language_.id).`in`(languageIds))
    }

    if (onlyStale) {
      predicates.add(
        cb.or(
          cb.isNull(translationJoin.get(Translation_.id)),
          cb.equal(translationJoin.get(Translation_.qaChecksStale), true),
        ),
      )
    }

    query.multiselect(keyRoot.get(Key_.id), langJoin.get(Language_.id))
    query.where(*predicates.toTypedArray())

    return entityManager.createQuery(query).resultList.map { tuple ->
      KeyLanguagePairView(
        keyId = tuple.get(0, Long::class.java),
        languageId = tuple.get(1, Long::class.java),
      )
    }
  }

  fun getKeyLanguagePairsByTranslationIds(translationIds: Collection<Long>): List<KeyLanguagePairView> {
    if (translationIds.isEmpty()) return emptyList()

    val cb = entityManager.criteriaBuilder
    val query = cb.createTupleQuery()
    val root = query.from(Translation::class.java)

    query.multiselect(
      root.get(Translation_.key).get<Long>("id"),
      root.get(Translation_.language).get<Long>("id"),
    )

    return translationIds.chunked(1000).flatMap { chunk ->
      query.where(root.get(Translation_.id).`in`(chunk))
      entityManager.createQuery(query).resultList.map { tuple ->
        KeyLanguagePairView(
          keyId = tuple.get(0, Long::class.java),
          languageId = tuple.get(1, Long::class.java),
        )
      }
    }
  }

  fun getBaseLanguageKeyIds(
    translationIds: Collection<Long>,
    baseLanguageId: Long,
  ): List<Long> {
    if (translationIds.isEmpty()) return emptyList()
    return translationIds.chunked(1000).flatMap { chunk ->
      entityManager
        .createQuery(
          "SELECT t.key.id FROM Translation t WHERE t.id IN :ids AND t.language.id = :baseLanguageId",
          Long::class.java,
        ).setParameter("ids", chunk)
        .setParameter("baseLanguageId", baseLanguageId)
        .resultList
    }
  }

  fun getSiblingIdsForBaseLanguageChanges(
    translationIds: Collection<Long>,
    baseLanguageId: Long,
  ): List<Long> {
    if (translationIds.isEmpty()) return emptyList()

    val translationIdSet = translationIds.toSet()

    val keyIds = getBaseLanguageKeyIds(translationIds, baseLanguageId)

    if (keyIds.isEmpty()) return emptyList()

    return keyIds
      .chunked(1000)
      .flatMap { keyChunk ->
        entityManager
          .createQuery(
            """
            SELECT t.id FROM Translation t
            WHERE t.key.id IN :keyIds
              AND t.language.id != :baseLanguageId
            """.trimIndent(),
            Long::class.java,
          ).setParameter("keyIds", keyChunk)
          .setParameter("baseLanguageId", baseLanguageId)
          .resultList
      }.filter { it !in translationIdSet }
  }
}
