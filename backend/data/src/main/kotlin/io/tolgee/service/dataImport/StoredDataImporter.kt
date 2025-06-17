package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.constants.Message
import io.tolgee.dtos.ImportResult
import io.tolgee.dtos.SimpleKeyResult
import io.tolgee.dtos.request.KeyDefinitionDto
import io.tolgee.dtos.request.ScreenshotInfoDto
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.dtos.request.key.KeyScreenshotDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Screenshot
import io.tolgee.model.UploadedImage
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Translation
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.flushAndClear
import io.tolgee.util.getSafeNamespace
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import kotlin.collections.forEach

class StoredDataImporter(
  applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE,
  private val reportStatus: ((ImportApplicationStatus) -> Unit)? = null,
  private val importSettings: IImportSettings,
  private val overrideMode: OverrideMode = OverrideMode.RECOMMENDED,
  private val errorOnFailedKey: Boolean? = null,
  // for single step import, we provide import data manager
  private val _importDataManager: ImportDataManager? = null,
  private val isSingleStepImport: Boolean = false,
  private val screenshots: List<ScreenshotToImport> = emptyList(),
  private val resolveConflict: ((ImportTranslation) -> ForceMode?)? = null,
) {
  private val importedKeys: MutableSet<Key> = mutableSetOf()
  private val importDataManager by lazy {
    if (_importDataManager != null) {
      return@lazy _importDataManager
    }
    ImportDataManager(applicationContext, import)
  }

  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val namespaceService = applicationContext.getBean(NamespaceService::class.java)

  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)

  private val securityService = applicationContext.getBean(SecurityService::class.java)

  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)

  private val authenticationFacade = applicationContext.getBean(AuthenticationFacade::class.java)

  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)

  val translationsToSave = mutableListOf<Pair<ImportTranslation, Translation>>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction,
   * looking for key in this map is also faster than querying database
   */
  private val keysToSave = mutableMapOf<Pair<String?, String>, Key>()

  private val keyMetasToSave: MutableList<KeyMeta> = mutableListOf()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction
   */
  private val translationService = applicationContext.getBean(TranslationService::class.java)

  private val entityManager = applicationContext.getBean(EntityManager::class.java)

  private val namespacesToSave = mutableMapOf<String?, Namespace>()

  private val newKeys = mutableListOf<Key>()

  /**
   * Keys where base translation was changed, so we need to set outdated flag on all translations
   */
  val outdatedFlagKeys: MutableList<Long> = mutableListOf()

  val failedKeyIds: MutableSet<Long> = mutableSetOf()

  /**
   * These are all keyMetas from all the keys to import. If multiple keys have same name and
   * namespace, the meta is merged to one, so there is only one meta for one key!!!
   *
   * It can be used only when we are finally importing the data. Before that, we cannot merge it.
   */
  private val storedMetas: MutableMap<Pair<String?, String>, KeyMeta> by lazy {
    val result: MutableMap<Pair<String?, String>, KeyMeta> = mutableMapOf()
    val metasToMerge =
      if (isSingleStepImport) {
        importDataManager.storedKeys.map { it.value.keyMeta }.filterNotNull()
      } else {
        keyMetaService.getWithFetchedData(this.import)
      }

    metasToMerge.forEach { currentKeyMeta ->
      val mapKey = currentKeyMeta.importKey!!.file.namespace to currentKeyMeta.importKey!!.name
      result[mapKey] = result[mapKey]?.let { existingKeyMeta ->
        keyMetaService.import(existingKeyMeta, currentKeyMeta)
        existingKeyMeta
      } ?: currentKeyMeta
    }
    result
  }

  fun doImport(): ImportResult {
    reportStatus?.invoke(ImportApplicationStatus.PREPARING_AND_VALIDATING)

    importDataManager.storedLanguages.forEach {
      it.prepareImport()
    }

    val shouldThrowError = errorOnFailedKey ?: when (forceMode) {
      ForceMode.NO_FORCE -> true
      else -> false
    }

    if (failedKeyIds.isNotEmpty() && shouldThrowError) {
      throw ImportConflictNotResolvedException(params = getFailedKeys(failedKeyIds))
    }

    addKeysAndCheckPermissions()

    handleKeyMetas()

    reportStatus?.invoke(ImportApplicationStatus.STORING_KEYS)

    namespaceService.saveAll(namespacesToSave.values)

    val keyEntitiesToSave = saveKeys()

    handlePluralization()

    saveKeyMetaData(keyEntitiesToSave)

    reportStatus?.invoke(ImportApplicationStatus.STORING_TRANSLATIONS)

    saveTranslations()

    reportStatus?.invoke(ImportApplicationStatus.FINALIZING)

    entityManager.flush()

    translationService.setOutdatedBatch(outdatedFlagKeys)

    tagNewKeys()

    entityManager.flush()

    deleteOtherKeys()

    entityManager.flushAndClear()

    addScreenshots()

    val failedKeys = if (failedKeyIds.isNotEmpty()) getFailedKeys(failedKeyIds) else null

    return ImportResult(failedKeys)
  }

  private fun addScreenshots(): Map<Long, Screenshot> {
    val uploadedImagesIds = screenshots.map { it -> it.screenshot.uploadedImageId }
    val images = imageUploadService.find(uploadedImagesIds)
    checkImageUploadPermissions(images)
    val createdScreenshots =
      images.associate {
        it.id to screenshotService.saveScreenshot(it)
      }

    val locations = images.map { it.location }

    val allReferences =
      screenshotService
        .getKeyScreenshotReferences(
          importedKeys.toList(),
          locations,
        ).toMutableList()

    val referencesToDelete = mutableListOf<KeyScreenshotReference>()

    screenshots.forEach {
      val screenshot = it.screenshot
      val key = getOrCreateKey(it.key)

      val screenshotResult =
        createdScreenshots[screenshot.uploadedImageId]
          ?: throw NotFoundException(Message.ONE_OR_MORE_IMAGES_NOT_FOUND)
      val info = ScreenshotInfoDto(screenshot.text, screenshot.positions)

      screenshotService.addReference(
        key = key.first,
        screenshot = screenshotResult.screenshot,
        info = info,
        originalDimension = screenshotResult.originalDimension,
        targetDimension = screenshotResult.targetDimension,
      )

      val toDelete =
        allReferences.filter { reference ->
          reference.key.id == key.first.id &&
            reference.screenshot.location == screenshotResult.screenshot.location
        }
      referencesToDelete.addAll(toDelete)
    }

    screenshotService.removeScreenshotReferences(referencesToDelete)

    return createdScreenshots
      .map { (uploadedImageId, screenshotResult) ->
        uploadedImageId to screenshotResult.screenshot
      }.toMap()
  }

  private fun getOrCreateKey(keyToImport: KeyDefinitionDto): Pair<Key, Boolean> {
    var isNew = false
    val key =
      importDataManager.existingKeys.computeIfAbsent(keyToImport.namespace to keyToImport.name) {
        isNew = true
        securityService.checkProjectPermission(import.project.id, Scope.KEYS_CREATE)
        keyService.createWithoutExistenceCheck(
          project = import.project,
          name = keyToImport.name,
          namespace = keyToImport.namespace,
          isPlural = false,
        )
      }
    return key to isNew
  }


  private fun checkImageUploadPermissions(images: List<UploadedImage>) {
    if (images.isNotEmpty()) {
      securityService.checkScreenshotsUploadPermission(import.project.id)
    }
    images.forEach { image ->
      if (authenticationFacade.authenticatedUser.id != image.userAccount.id) {
        throw PermissionException(Message.CURRENT_USER_DOES_NOT_OWN_IMAGE)
      }
    }
  }

  private fun tagNewKeys() {
    (importSettings as? SingleStepImportRequest)?.tagNewKeys?.let { tagNewKeys ->
      tagService.tagKeys(newKeys.associateWith { tagNewKeys })
    }
  }

  private fun deleteOtherKeys() {
    if ((importSettings as? SingleStepImportRequest)?.removeOtherKeys == true) {
      val namespaces = import.files.map { getSafeNamespace(it.namespace) }.toSet()
      val existingKeys = importDataManager.existingKeys.entries.filter {
        // taking only keys from namespaces that are included in the import
        namespaces.contains(getSafeNamespace(it.value.namespace?.name))
      }
      val importedKeys = importDataManager.storedKeys.entries.map { (pair) -> Pair(pair.first.namespace, pair.second) }
      val otherKeys = existingKeys.filter { existing -> !importedKeys.contains(existing.key) }
      if (otherKeys.isNotEmpty()) {
        keyService.deleteMultiple(otherKeys.map { it.value.id })
      }
    }
  }

  private fun handlePluralization() {
    val handler = PluralizationHandler(importDataManager, this, translationService)
    handler.handlePluralization()
    saveKeys(handler.keysToSave)
  }

  private fun saveKeyMetaData(keyEntitiesToSave: Collection<Key>) {
    // hibernate bug workaround:
    // saving key metas will cause them to be recreated by hibernate with empty values
    // we have to save references to comments and codeReferences before saving key metas
    val comments =
      keyEntitiesToSave.flatMap {
        it.keyMeta?.comments ?: emptyList()
      }
    val codeReferences =
      keyEntitiesToSave.flatMap {
        it.keyMeta?.codeReferences ?: emptyList()
      }
    keyMetaService.saveAll(keyMetasToSave)
    keyMetaService.saveAllComments(comments)
    keyMetaService.saveAllCodeReferences(codeReferences)

    // set links to comments and code references to point to correct (previous)
    // instances instead of the new empty ones
    comments.groupBy { it.keyMeta }.forEach { (keyMeta, comments) ->
      keyMeta.comments = comments.toMutableList()
    }
    codeReferences.groupBy { it.keyMeta }.forEach { (keyMeta, codeReferences) ->
      keyMeta.codeReferences = codeReferences.toMutableList()
    }
  }

  private fun saveTranslations() {
    checkTranslationPermissions()
    translationService.saveAll(translationsToSave.map { it.second })
  }

  private fun getFailedKeys(ids: Set<Long>): List<SimpleKeyResult> {
    return keyService.find(ids).map { it.toSimpleKey() }
  }

  private fun saveKeys(): Collection<Key> {
    return saveKeys(keysToSave.values)
  }

  private fun saveKeys(keys: Collection<Key>): Collection<Key> {
    keyService.saveAll(keys)
    return keys
  }

  private fun addKeysAndCheckPermissions() {
    addAllKeys()
    checkKeyPermissions()
  }

  private fun checkTranslationPermissions() {
    val langs = translationsToSave.map { it.second.language }.toSet().map { it.id }
    securityService.checkLanguageTranslatePermission(import.project.id, langs)
  }

  private fun checkKeyPermissions() {
    val isCreatingKey = keysToSave.values.any { it.id == 0L }
    if (isCreatingKey) {
      securityService.checkProjectPermission(import.project.id, Scope.KEYS_CREATE)
    }
  }

  private fun handleKeyMetas() {
    this.importDataManager.storedKeys.entries.forEach { (fileNamePair, importKey) ->
      if (!importKey.shouldBeImported) {
        return@forEach
      }
      val importedKeyMeta = storedMetas[fileNamePair.first.namespace to importKey.name]
      // don't touch key meta when imported key has no meta
      if (importedKeyMeta != null) {
        keysToSave[fileNamePair.first.namespace to importKey.name]?.let { newKey ->
          // if key is obtained or created and meta exists, take it and import the data from the imported one
          // persist is cascaded on key, so it should be fine
          val keyMeta =
            importDataManager.existingMetas[fileNamePair.first.namespace to importKey.name]?.also {
              keyMetaService.import(it, importedKeyMeta, importSettings.overrideKeyDescriptions)
            } ?: importedKeyMeta
          // also set key and remove import key
          keyMeta.also {
            it.key = newKey
            it.importKey = null
          }
          // assign new meta
          newKey.keyMeta = keyMeta
          keyMetasToSave.add(keyMeta)
        }
      }
    }
  }

  private fun addAllKeys() {
    importDataManager.storedKeys.map { (fileNamePair, importKey) ->
      if (!importKey.shouldBeImported) {
        return@map
      }
      addKeyToSave(importKey.file.namespace, importKey.name)
    }
  }

  private fun ImportLanguage.prepareImport() {
    importDataManager.populateStoredTranslations(this)
    importDataManager.handleConflicts(true)
    importDataManager.applyKeyCreateChange(importSettings.createNewKeys)
    importDataManager.getStoredTranslations(this).forEach { it.doImport() }
  }

  private fun ImportTranslation.doImport() {
    if (!this.isSelectedToImport || !this.key.shouldBeImported || this.language.ignored) {
      return
    }

    val resolution = this.getConflictResolution()

    if (resolution == ForceMode.KEEP) {
      return
    }

    if (resolution == ForceMode.NO_FORCE) {
      failedKeyIds.add(this.existingKey.id)
      return
    }

    val language =
      this.language.existingLanguage
        ?: throw BadRequestException(Message.EXISTING_LANGUAGE_NOT_SELECTED)
    val translation =
      this.conflict ?: Translation().apply {
        this.language = language
      }

    this@StoredDataImporter.importedKeys.add(existingKey)
    translation.key = existingKey
    if (language == language.project.baseLanguage && translation.text != this.text) {
      outdatedFlagKeys.add(translation.key.id)
    }
    translationService.setTranslationTextNoSave(translation, text)
    translationsToSave.add(this to translation)
  }

  private val ImportTranslation.existingKey: Key
    get() {
      // get key from already saved keys to save
      return keysToSave.computeIfAbsent(this.key.file.namespace to this.key.name) {
        // or get it from conflict or create new one
        val newKey =
          importDataManager.existingKeys[this.key.file.namespace to this.key.name]
            ?: createNewKey(this.key.name, this.key.file.namespace)
        newKey
      }
    }

  private fun addKeyToSave(
    namespace: String?,
    keyName: String,
  ): Key {
    return keysToSave.computeIfAbsent(namespace to keyName) {
      importDataManager.existingKeys[namespace to keyName] ?: createNewKey(keyName, namespace)
    }
  }

  private fun createNewKey(
    name: String,
    namespace: String?,
  ): Key {
    return Key(name = name).apply {
      project = import.project
      this.namespace = getNamespace(namespace)
      newKeys.add(this)
    }
  }

  private fun ImportTranslation.getConflictResolution(): ForceMode {
    if (this.conflict?.text == null) {
      return ForceMode.OVERRIDE
    }
    if (this.resolved) {
      return if (this.override) ForceMode.OVERRIDE else ForceMode.KEEP
    }
    val currentForceMode = resolveConflict?.invoke(this) ?: forceMode
    if (currentForceMode == ForceMode.KEEP) {
      return ForceMode.KEEP
    }
    if (currentForceMode == ForceMode.OVERRIDE) {
      if (
        (overrideMode == OverrideMode.ALL) &&
        ImportTranslationView.isOverridableWithAll(this.conflictType)
      ) {
        return ForceMode.OVERRIDE
      }
      if (
        (overrideMode == OverrideMode.RECOMMENDED) &&
        ImportTranslationView.isOverridableWithRecommended(this.conflictType)
      ) {
        return ForceMode.OVERRIDE
      }
    }
    return ForceMode.NO_FORCE
  }

  private fun getNamespace(name: String?): Namespace? {
    name ?: return null
    return importDataManager.existingNamespaces[name] ?: namespacesToSave.computeIfAbsent(name) {
      Namespace(name, import.project)
    }
  }

  private val tagService by lazy {
    applicationContext.getBean(TagService::class.java)
  }

  companion object {
    data class ScreenshotToImport(
      val key: KeyDefinitionDto,
      val screenshot: KeyScreenshotDto
    )
  }
}
