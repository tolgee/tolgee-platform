package io.tolgee.component

import io.tolgee.activity.ActivityHolder
import io.tolgee.activity.data.ActivityType
import io.tolgee.dtos.request.key.ComplexEditKeyDto
import io.tolgee.hateoas.key.KeyWithDataModel
import io.tolgee.hateoas.key.KeyWithDataModelAssembler
import io.tolgee.model.Project
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.getSafeNamespace
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import kotlin.properties.Delegates

class KeyComplexEditHelper(
  applicationContext: ApplicationContext,
  private val keyId: Long,
  private val dto: ComplexEditKeyDto
) {
  private val keyWithDataModelAssembler: KeyWithDataModelAssembler =
    applicationContext.getBean(KeyWithDataModelAssembler::class.java)
  private val keyService: KeyService = applicationContext.getBean(KeyService::class.java)
  private val securityService: SecurityService = applicationContext.getBean(SecurityService::class.java)
  private val languageService: LanguageService = applicationContext.getBean(LanguageService::class.java)
  private val projectHolder: ProjectHolder = applicationContext.getBean(ProjectHolder::class.java)
  private val translationService: TranslationService = applicationContext.getBean(TranslationService::class.java)
  private val tagService: TagService = applicationContext.getBean(TagService::class.java)
  private val screenshotService: ScreenshotService = applicationContext.getBean(ScreenshotService::class.java)
  private val activityHolder: ActivityHolder = applicationContext.getBean(ActivityHolder::class.java)
  private val transactionManager: PlatformTransactionManager =
    applicationContext.getBean(PlatformTransactionManager::class.java)

  private lateinit var key: Key
  private var modifiedTranslations: Map<String, String?>? = null
  private val dtoTags = dto.tags

  private var areTranslationsModified by Delegates.notNull<Boolean>()
  private var areTagsModified by Delegates.notNull<Boolean>()
  private var isKeyModified by Delegates.notNull<Boolean>()
  private var isScreenshotDeleted by Delegates.notNull<Boolean>()
  private var isScreenshotAdded by Delegates.notNull<Boolean>()

  fun doComplexUpdate(): KeyWithDataModel {
    return executeInNewTransaction(transactionManager = transactionManager) {
      prepareData()
      prepareConditions()
      setActivityHolder()

      if (modifiedTranslations != null && areTranslationsModified) {
        projectHolder.projectEntity.checkTranslationsEditPermission()
        securityService.checkLanguageTagPermissions(modifiedTranslations!!.keys, projectHolder.project.id)
        translationService.setForKey(key, translations = modifiedTranslations!!)
      }

      if (dtoTags !== null && areTagsModified) {
        key.project.checkKeysEditPermission()
        tagService.updateTags(key, dtoTags)
      }

      if (isScreenshotAdded || isScreenshotDeleted) {
        updateScreenshotsWithPermissionCheck(dto, key)
      }

      var edited = key

      if (isKeyModified) {
        key.project.checkKeysEditPermission()
        edited = keyService.edit(key, dto.name, dto.namespace)
      }

      keyWithDataModelAssembler.toModel(edited)
    }
  }

  private fun setActivityHolder() {
    if (!isSingleOperation) {
      activityHolder.activity = ActivityType.COMPLEX_EDIT
      return
    }

    if (areTranslationsModified) {
      activityHolder.activity = ActivityType.SET_TRANSLATIONS
      return
    }

    if (areTagsModified) {
      activityHolder.activity = ActivityType.KEY_TAGS_EDIT
      return
    }

    if (isKeyModified) {
      activityHolder.activity = ActivityType.KEY_NAME_EDIT
      return
    }

    if (isScreenshotAdded) {
      activityHolder.activity = ActivityType.SCREENSHOT_ADD
      return
    }

    if (isScreenshotDeleted) {
      activityHolder.activity = ActivityType.SCREENSHOT_DELETE
      return
    }
  }

  private val isSingleOperation: Boolean
    get() {
      return arrayOf(
        areTranslationsModified,
        areTagsModified,
        isKeyModified,
        isScreenshotAdded,
        isScreenshotDeleted
      ).sumOf { if (it) 1 as Int else 0 } == 0
    }

  private fun prepareData() {
    key = keyService.get(keyId)
    key.checkInProject()
    modifiedTranslations = dto.translations?.let { dtoTranslations -> filterModifiedOnly(dtoTranslations, key) }
  }

  private fun prepareConditions() {
    areTranslationsModified = !modifiedTranslations.isNullOrEmpty()
    areTagsModified = dtoTags != null && areTagsModified(key, dtoTags)
    isKeyModified = key.name != dto.name || getSafeNamespace(key.namespace?.name) != getSafeNamespace(dto.namespace)
    isScreenshotDeleted = !dto.screenshotIdsToDelete.isNullOrEmpty()
    isScreenshotAdded = !dto.screenshotUploadedImageIds.isNullOrEmpty() || !dto.screenshotsToAdd.isNullOrEmpty()
  }

  private fun areTagsModified(
    key: Key,
    dtoTags: List<String>
  ): Boolean {
    val currentTags = key.keyMeta?.tags?.map { it.name } ?: listOf()
    val currentTagsContainAllNewTags = currentTags.containsAll(dtoTags)
    val newTagsContainAllCurrentTags = dtoTags.containsAll(currentTags)

    return !currentTagsContainAllNewTags || !newTagsContainAllCurrentTags
  }

  private fun filterModifiedOnly(
    dtoTranslations: Map<String, String?>,
    key: Key?
  ): Map<String, String?> {
    val languages = languageService.findByTags(dtoTranslations.keys, projectHolder.project.id)
    val existingTranslations = translationService.getKeyTranslations(
      languages,
      projectHolder.projectEntity,
      key
    ).associate { it.language.tag to it.text }

    return dtoTranslations.filter { it.value != existingTranslations[it.key] }
  }

  private fun updateScreenshotsWithPermissionCheck(dto: ComplexEditKeyDto, key: Key) {
    dto.screenshotIdsToDelete?.let { screenshotIds ->
      deleteScreenshots(screenshotIds, key)
    }

    addScreenshots(key, dto)
  }

  private fun addScreenshots(key: Key, dto: ComplexEditKeyDto) {
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
    key: Key
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
