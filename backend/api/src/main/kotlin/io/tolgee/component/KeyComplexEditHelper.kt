package io.tolgee.component

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.constants.Message
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.hateoas.key.KeyWithDataModel
import io.tolgee.hateoas.key.KeyWithDataModelAssembler
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.executeInNewRepeatableTransaction
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import kotlin.properties.Delegates

class KeyComplexEditHelper(
  applicationContext: ApplicationContext,
  private val keyId: Long,
  private val dto: ComplexEditKeyDto,
) {
  private val keyWithDataModelAssembler: KeyWithDataModelAssembler =
    applicationContext.getBean(KeyWithDataModelAssembler::class.java)
  private val keyService: KeyService = applicationContext.getBean(KeyService::class.java)
  private val securityService: SecurityService = applicationContext.getBean(SecurityService::class.java)
  private val languageService: LanguageService = applicationContext.getBean(LanguageService::class.java)
  private val projectHolder: ProjectHolder = applicationContext.getBean(ProjectHolder::class.java)
  private val translationService: TranslationService = applicationContext.getBean(TranslationService::class.java)
  private val tagService: TagService = applicationContext.getBean(TagService::class.java)
  private val objectMapper: ObjectMapper = applicationContext.getBean(ObjectMapper::class.java)
  private val screenshotService: ScreenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val activityHolder: ActivityHolder = applicationContext.getBean(ActivityHolder::class.java)
  private val transactionManager: PlatformTransactionManager =
    applicationContext.getBean(PlatformTransactionManager::class.java)
  private val bigMetaService = applicationContext.getBean(BigMetaService::class.java)
  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)
  private val keyCustomValuesValidator = applicationContext.getBean(KeyCustomValuesValidator::class.java)

  private lateinit var key: Key
  private var modifiedTranslations: Map<Long, String?>? = null
  private var modifiedStates: Map<Long, TranslationState>? = mapOf()
  private val dtoTags = dto.tags

  private var areTranslationsModified by Delegates.notNull<Boolean>()
  private var areStatesModified by Delegates.notNull<Boolean>()
  private var areTagsModified by Delegates.notNull<Boolean>()
  private var isKeyNameModified by Delegates.notNull<Boolean>()
  private var isScreenshotDeleted by Delegates.notNull<Boolean>()
  private var isScreenshotAdded by Delegates.notNull<Boolean>()
  private var isBigMetaProvided by Delegates.notNull<Boolean>()
  private var isNamespaceChanged: Boolean = false
  private var isCustomDataChanged: Boolean = false
  private var isDescriptionChanged: Boolean = false
  private var isIsPluralChanged: Boolean = false
  private var newIsPlural by Delegates.notNull<Boolean>()

  private val languages by lazy {
    val translationLanguages = dto.translations?.keys ?: setOf()
    val stateLanguages = dto.states?.keys ?: setOf()

    val all = (translationLanguages + stateLanguages)
    if (all.isEmpty()) {
      return@lazy setOf()
    }
    languageService.findEntitiesByTags(all, projectHolder.project.id)
  }

  private val existingTranslations: MutableMap<String, Translation> by lazy {
    translationService.getKeyTranslations(
      languages,
      projectHolder.projectEntity,
      key,
    ).associateBy { it.language.tag }.toMutableMap()
  }

  fun doComplexUpdate(): KeyWithDataModel {
    // we don't want phantoms, since we are updating all the translations when isPlural is changed
    return executeInNewRepeatableTransaction(
      transactionManager = transactionManager,
      isolationLevel = TransactionDefinition.ISOLATION_SERIALIZABLE,
    ) {
      prepareData()
      prepareConditions()
      setActivityHolder()
      doTranslationsUpdate()
      doStateUpdate()
      doUpdateTags()
      doUpdateScreenshots()
      val result = doUpdateKey()
      storeBigMeta()
      result
    }
  }

  private fun storeBigMeta() {
    if (isBigMetaProvided) {
      securityService.checkBigMetaUploadPermission(projectHolder.project.id)
      bigMetaService.store(dto.relatedKeysInOrder!!, projectHolder.projectEntity)
    }
  }

  private fun doUpdateKey(): KeyWithDataModel {
    var edited = key

    if (requireKeyEditPermission) {
      key.project.checkKeysEditPermission()
    }

    if (isDescriptionChanged) {
      keyMetaService.getOrCreateForKey(key).apply {
        description = dto.description
      }
    }

    if (isIsPluralChanged) {
      key.isPlural = dto.isPlural!!
      key.pluralArgName = dto.pluralArgName ?: key.pluralArgName
      translationService.onKeyIsPluralChanged(
        mapOf(key.id to newPluralArgName),
        dto.isPlural!!,
        throwOnDataLoss = dto.warnOnDataLoss ?: false,
      )
      keyService.save(key)
    }

    if (isCustomDataChanged) {
      dto.custom?.let { newCustomValues ->
        keyCustomValuesValidator.validate(newCustomValues)
        val keyMeta = keyMetaService.getOrCreateForKey(key)
        keyMeta.custom = newCustomValues.toMutableMap()
        keyMetaService.save(keyMeta)
      }
    }

    if (isKeyNameModified || isNamespaceChanged) {
      edited = keyService.edit(key, dto.name, dto.namespace)
    }

    return keyWithDataModelAssembler.toModel(edited)
  }

  private fun doUpdateScreenshots() {
    if (isScreenshotAdded || isScreenshotDeleted) {
      updateScreenshotsWithPermissionCheck(dto, key)
    }
  }

  private fun doUpdateTags() {
    if (dtoTags !== null && areTagsModified) {
      activityHolder.businessEventData["usesTags"] = "true"
      key.project.checkKeysEditPermission()
      tagService.updateTags(key, dtoTags)
    }
  }

  private fun doStateUpdate() {
    if (areStatesModified) {
      securityService.checkLanguageChangeStatePermissionsByLanguageId(
        modifiedStates!!.keys,
        projectHolder.project.id,
        key.id,
      )
      translationService.setStateBatch(
        states =
          modifiedStates!!.map {
            val translation =
              existingTranslations[languageById(it.key).tag] ?: throw NotFoundException(
                Message.TRANSLATION_NOT_FOUND,
              )

            translation to it.value
          }.toMap(),
      )
    }
  }

  private fun doTranslationsUpdate() {
    if (modifiedTranslations != null && areTranslationsModified) {
      securityService.checkLanguageTranslatePermissionsByLanguageId(
        modifiedTranslations!!.keys,
        projectHolder.project.id,
        keyId,
      )

      val modifiedTranslations = getModifiedTranslationsByTag()
      val normalizedPlurals = validateAndNormalizePlurals(modifiedTranslations)

      val existingTranslationsByTag = getExistingTranslationsByTag()
      val oldTranslations =
        modifiedTranslations.map {
          it.key to existingTranslationsByTag[it.key]
        }.toMap()

      val translations =
        translationService.setForKey(
          key,
          oldTranslations = oldTranslations,
          translations = normalizedPlurals,
        )

      translations.forEach {
        if (existingTranslations[it.key.tag] == null) {
          existingTranslations[it.key.tag] = it.value
        }
      }
    }
  }

  private fun validateAndNormalizePlurals(modifiedTranslations: Map<Language, String?>): Map<Language, String?> {
    if (newIsPlural) {
      return translationService.validateAndNormalizePlurals(modifiedTranslations, newPluralArgName)
    }
    return modifiedTranslations
  }

  private val newPluralArgName: String? by lazy {
    dto.pluralArgName ?: key.pluralArgName
  }

  private fun getExistingTranslationsByTag() =
    existingTranslations.map { languageByTag(it.key) to it.value.text }.toMap()

  private fun getModifiedTranslationsByTag() =
    modifiedTranslations!!
      .map { languageById(it.key) to it.value }
      .toMap()

  private fun setActivityHolder() {
    activityHolder.activity = getActivityType()
  }

  private fun getActivityType(): ActivityType {
    val possible = getPossibleOperations()
    val singlePossible = possible.singleOrNull()

    return singlePossible ?: ActivityType.COMPLEX_EDIT
  }

  private fun getPossibleOperations(): MutableList<ActivityType> {
    val possibleOperations = mutableListOf<ActivityType>()

    if (areTranslationsModified) {
      possibleOperations.add(ActivityType.SET_TRANSLATIONS)
    }

    if (areStatesModified) {
      possibleOperations.add(ActivityType.SET_TRANSLATION_STATE)
    }

    if (areTagsModified) {
      possibleOperations.add(ActivityType.KEY_TAGS_EDIT)
    }

    if (isKeyNameModified) {
      possibleOperations.add(ActivityType.KEY_NAME_EDIT)
    }

    if (isScreenshotAdded) {
      possibleOperations.add(ActivityType.SCREENSHOT_ADD)
    }

    if (isScreenshotDeleted) {
      possibleOperations.add(ActivityType.SCREENSHOT_DELETE)
    }

    return possibleOperations
  }

  private fun prepareData() {
    key = keyService.get(keyId)
    key.checkInProject()
    prepareModifiedTranslations()
    prepareModifiedStates()
    newIsPlural = dto.isPlural ?: key.isPlural
  }

  private fun prepareConditions() {
    areTranslationsModified = !modifiedTranslations.isNullOrEmpty()
    areStatesModified = !modifiedStates.isNullOrEmpty()
    areTagsModified = dtoTags != null && areTagsModified(key, dtoTags)
    isKeyNameModified = key.name != dto.name
    isNamespaceChanged = key.namespace?.name != dto.namespace
    isDescriptionChanged = key.keyMeta?.description != dto.description
    isIsPluralChanged =
      dto.isPlural != null && key.isPlural != dto.isPlural ||
      (dto.isPlural == true && key.pluralArgName != dto.pluralArgName)
    isCustomDataChanged = dto.custom != null &&
      objectMapper.writeValueAsString(key.keyMeta?.custom) != objectMapper.writeValueAsString(dto.custom)
    isScreenshotDeleted = !dto.screenshotIdsToDelete.isNullOrEmpty()
    isScreenshotAdded = !dto.screenshotUploadedImageIds.isNullOrEmpty() || !dto.screenshotsToAdd.isNullOrEmpty()
    isBigMetaProvided = !dto.relatedKeysInOrder.isNullOrEmpty()
  }

  private fun areTagsModified(
    key: Key,
    dtoTags: List<String>,
  ): Boolean {
    val currentTags = key.keyMeta?.tags?.map { it.name } ?: listOf()
    val currentTagsContainAllNewTags = currentTags.containsAll(dtoTags)
    val newTagsContainAllCurrentTags = dtoTags.containsAll(currentTags)

    return !currentTagsContainAllNewTags || !newTagsContainAllCurrentTags
  }

  val requireKeyEditPermission
    get() =
      isKeyNameModified ||
        isNamespaceChanged ||
        isDescriptionChanged ||
        isIsPluralChanged ||
        isCustomDataChanged

  private fun prepareModifiedTranslations() {
    modifiedTranslations =
      dto.translations?.filter { it.value != existingTranslations[it.key]?.text }
        ?.mapKeys { languageByTag(it.key).id }
  }

  private fun prepareModifiedStates() {
    modifiedStates =
      dto.states?.filter { it.value.translationState != existingTranslations[it.key]?.state }
        ?.map { languageByTag(it.key).id to it.value.translationState }?.toMap()
  }

  private fun languageByTag(tag: String): Language {
    return languages.find { it.tag == tag } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  private fun languageById(id: Long): Language {
    return languages.find { it.id == id } ?: throw NotFoundException(Message.LANGUAGE_NOT_FOUND)
  }

  private fun updateScreenshotsWithPermissionCheck(
    dto: ComplexEditKeyDto,
    key: Key,
  ) {
    dto.screenshotIdsToDelete?.let { screenshotIds ->
      deleteScreenshots(screenshotIds, key)
    }

    addScreenshots(key, dto)
  }

  private fun addScreenshots(
    key: Key,
    dto: ComplexEditKeyDto,
  ) {
    if (isScreenshotAdded) {
      key.project.checkScreenshotsUploadPermission()
    }

    val screenshotUploadedImageIds = dto.screenshotUploadedImageIds
    if (screenshotUploadedImageIds != null) {
      screenshotService.saveUploadedImages(screenshotUploadedImageIds, key)
      return
    }

    val screenshotsToAdd = dto.screenshotsToAdd
    if (screenshotsToAdd != null) {
      screenshotService.saveUploadedImages(screenshotsToAdd, key)
    }
  }

  private fun deleteScreenshots(
    screenshotIds: List<Long>,
    key: Key,
  ) {
    if (screenshotIds.isNotEmpty()) {
      key.project.checkScreenshotsDeletePermission()
    }
    screenshotService.findByIdIn(screenshotIds).forEach {
      screenshotService.removeScreenshotReference(key, it)
    }
  }

  private fun Project.checkScreenshotsDeletePermission() {
    securityService.checkProjectPermission(this.id, Scope.SCREENSHOTS_DELETE)
  }

  private fun Project.checkKeysEditPermission() {
    securityService.checkProjectPermission(this.id, Scope.KEYS_EDIT)
  }

  private fun Project.checkTranslationsEditPermission() {
    securityService.checkProjectPermission(this.id, Scope.TRANSLATIONS_EDIT)
  }

  private fun Key.checkInProject() {
    keyService.checkInProject(this, projectHolder.project.id)
  }

  private fun Project.checkScreenshotsUploadPermission() {
    securityService.checkScreenshotsUploadPermission(this.id)
  }
}
