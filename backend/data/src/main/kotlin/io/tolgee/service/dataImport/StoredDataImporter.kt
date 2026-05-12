package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.configuration.tolgee.ImportProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.ImportResult
import io.tolgee.dtos.dataImport.SimpleImportConflictResult
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.ConflictType
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.translation.Translation
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.ImageUploadService
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.service.dataImport.status.ImportApplicationStatusItem
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.flushAndClear
import io.tolgee.util.getSafeNamespace
import io.tolgee.util.nullIfEmpty
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import java.util.IdentityHashMap

class StoredDataImporter(
  private val applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE,
  private val reportStatus: ((ImportApplicationStatusItem) -> Unit) = {},
  private val importSettings: IImportSettings,
  private val overrideMode: OverrideMode = OverrideMode.RECOMMENDED,
  private val errorOnUnresolvedConflict: Boolean? = null,
  // for single step import, we provide import data manager
  private val _importDataManager: ImportDataManager? = null,
  private val isSingleStepImport: Boolean = false,
  private val screenshots: List<ScreenshotImporter.Companion.ScreenshotToImport> = emptyList(),
  private val resolveConflict: ((ImportTranslation) -> ForceMode?)? = null,
) {
  private val importDataManager by lazy {
    if (_importDataManager != null) {
      return@lazy _importDataManager
    }
    ImportDataManager(applicationContext, import)
  }

  private val screenshotImporter by lazy { ScreenshotImporter(applicationContext) }

  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val namespaceService = applicationContext.getBean(NamespaceService::class.java)

  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)

  private val securityService = applicationContext.getBean(SecurityService::class.java)

  private val imageUploadService = applicationContext.getBean(ImageUploadService::class.java)

  private val authenticationFacade = applicationContext.getBean(AuthenticationFacade::class.java)

  private val importProperties = applicationContext.getBean(ImportProperties::class.java)

  private val batchSize: Int
    get() = importProperties.flushBatchSize

  private val screenshotService = applicationContext.getBean(ScreenshotService::class.java)

  val translationsToSave = mutableListOf<Translation>()

  private val translationLanguageIds = mutableSetOf<Long>()

  /**
   * Keys whose plural status the import is flipping from non-plural to
   * plural — (namespace, name) → source `pluralArgName`. Already-plural
   * DB keys are handled directly from [importDataManager.existingKeys]
   * in [PluralizationHandler].
   */
  val pluralFlipKeys = HashMap<Pair<String?, String>, String?>()

  /**
   * Nested namespace → name → Key. Filled while checking conflicts and
   * persisted once everything has been validated — a thrown
   * `ImportConflictNotResolvedException` commits the transaction and
   * we don't want partial data on disk.
   */
  private val keysToSave = HashMap<String?, HashMap<String, Key>>()

  private fun keysToSavePut(
    namespace: String?,
    name: String,
    key: Key,
  ): Key {
    keysToSave.getOrPut(namespace) { HashMap() }[name] = key
    return key
  }

  private fun keysToSaveGet(
    namespace: String?,
    name: String,
  ): Key? = keysToSave[namespace]?.get(name)

  private fun keysToSaveValuesSequence(): Sequence<Key> =
    keysToSave.values.asSequence().flatMap { it.values.asSequence() }

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

  val unresolvedConflicts: MutableList<ImportTranslation> = mutableListOf()

  /** Imported (namespace, keyName) pairs, captured before parsed-file data is released. */
  private var importedKeyPairs: List<Pair<String?, String>> = emptyList()

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
        importDataManager.storedKeysValuesSequence().mapNotNull { it.keyMeta }.toList()
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
    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.PREPARING_AND_VALIDATING))

    importDataManager.storedLanguages.forEach { lang ->
      lang.prepareImport()
      importDataManager.releaseLanguageData(lang)
    }

    throwOnUnresolvedConflicts(unresolvedConflicts)

    checkTranslationPermissions()

    addKeysAndCheckPermissions()

    handleKeyMetas()

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.STORING_KEYS))

    namespaceService.saveAll(namespacesToSave.values)

    addScreenshots()
    releaseNewKeysReferences()

    handlePluralization()

    importedKeyPairs =
      importDataManager.storedKeys.entries.flatMap { (file, byName) ->
        byName.keys.map { name -> file.namespace to name }
      }
    importDataManager.releaseData()

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.STORING_TRANSLATIONS))

    saveKeysAndTranslationsInBatches()

    reportStatus(ImportApplicationStatusItem(ImportApplicationStatus.FINALIZING))

    translationService.setOutdatedBatch(outdatedFlagKeys)

    entityManager.flush()

    deleteOtherKeys()

    entityManager.flushAndClear()

    return ImportResult(
      unresolvedConflicts.nullIfEmpty()?.let {
        getUnresolvedConflicts(unresolvedConflicts)
      },
    )
  }

  private fun addScreenshots() {
    screenshotImporter.importScreenshots(
      screenshots,
      existingKeys = importDataManager.existingKeys.values.toList(),
      newKeys = newKeys,
      addKeyToSave = { namespace: String?, key: String ->
        this.addKeyToSave(namespace, key)
      },
      projectId = import.project.id,
    )
  }

  private fun releaseNewKeysReferences() {
    newKeys.clear()
  }

  /**
   * Throws error when `errorOnUnresolvedConflict` is set on the API request
   * and there are some unresolved conflicts
   */
  private fun throwOnUnresolvedConflicts(unresolvedConflicts: List<ImportTranslation>) {
    // when force mode is `NO_FORCE` default value is true, otherwise false
    val shouldThrowError =
      errorOnUnresolvedConflict ?: when (forceMode) {
        ForceMode.NO_FORCE -> true
        else -> false
      }

    if (unresolvedConflicts.isNotEmpty() && shouldThrowError) {
      throw ImportConflictNotResolvedException(params = getUnresolvedConflicts(unresolvedConflicts))
    }
  }

  private fun tagNewKeysInBatch(newKeysInBatch: List<Key>) {
    if (newKeysInBatch.isEmpty()) return
    val tagsToApply = (importSettings as? SingleStepImportRequest)?.tagNewKeys ?: return
    if (tagsToApply.isEmpty()) return
    tagService.tagKeys(newKeysInBatch.associateWith { tagsToApply })
  }

  private fun deleteOtherKeys() {
    if ((importSettings as? SingleStepImportRequest)?.removeOtherKeys == true) {
      val namespaces = import.files.map { getSafeNamespace(it.namespace) }.toSet()
      val existingKeys =
        importDataManager.existingKeys.entries.filter {
          // taking only keys from namespaces that are included in the import
          namespaces.contains(getSafeNamespace(it.value.namespace?.name))
        }
      val importedKeys = importedKeyPairs
      val otherKeys = existingKeys.filter { existing -> !importedKeys.contains(existing.key) }
      if (otherKeys.isNotEmpty()) {
        keyService.hardDeleteMultiple(otherKeys.map { it.value.id })
      }
    }
  }

  private fun handlePluralization() {
    val handler = PluralizationHandler(importDataManager, this, translationService)
    handler.handlePluralization()
    saveKeys(handler.keysToSave)
  }

  private fun saveKeysAndTranslationsInBatches() {
    // IdentityHashMap because Key.equals isn't overridden (default identity)
    // but Key.hashCode is — the inconsistency would make a regular HashMap
    // unreliable.
    val translationsByKey = IdentityHashMap<Key, MutableList<Translation>>(keysToSave.size)
    translationsToSave.forEach { t ->
      translationsByKey.getOrPut(t.key) { ArrayList(2) }.add(t)
    }
    translationsToSave.clear()

    val keyMetaByKey = IdentityHashMap<Key, KeyMeta>(keyMetasToSave.size)
    keyMetasToSave.forEach { keyMetaByKey[it.key!!] = it }
    keyMetasToSave.clear()

    val keyBatch = ArrayList<Key>(batchSize)
    val outerIter = keysToSave.entries.iterator()
    while (outerIter.hasNext()) {
      val (_, innerMap) = outerIter.next()
      val innerIter = innerMap.entries.iterator()
      while (innerIter.hasNext()) {
        keyBatch.add(innerIter.next().value)
        innerIter.remove()
        if (keyBatch.size == batchSize) {
          flushBatch(keyBatch, translationsByKey, keyMetaByKey)
          keyBatch.clear()
        }
      }
      outerIter.remove()
    }
    if (keyBatch.isNotEmpty()) {
      flushBatch(keyBatch, translationsByKey, keyMetaByKey)
    }
  }

  private fun flushBatch(
    keyBatch: List<Key>,
    translationsByKey: IdentityHashMap<Key, MutableList<Translation>>,
    keyMetaByKey: IdentityHashMap<Key, KeyMeta>,
  ) {
    val transientKeysInBatch = keyBatch.filter { it.id == 0L }

    keyService.saveAll(keyBatch)
    saveKeyMetaDataForBatch(keyBatch, keyMetaByKey)

    val batchTranslations = ArrayList<Translation>(keyBatch.size * 2)
    for (key in keyBatch) {
      val list = translationsByKey.remove(key) ?: continue
      batchTranslations.addAll(list)
    }
    translationService.saveAll(batchTranslations)

    tagNewKeysInBatch(transientKeysInBatch)

    entityManager.flushAndClear()
  }

  private fun saveKeyMetaDataForBatch(
    keyBatch: Collection<Key>,
    keyMetaByKey: IdentityHashMap<Key, KeyMeta>,
  ) {
    val batchMetas = keyBatch.mapNotNull { keyMetaByKey.remove(it) }
    if (batchMetas.isEmpty()) return

    // hibernate bug workaround:
    // saving key metas will cause them to be recreated by hibernate with empty values
    // we have to save references to comments and codeReferences before saving key metas
    val comments = batchMetas.flatMap { it.comments }
    val codeReferences = batchMetas.flatMap { it.codeReferences }
    keyMetaService.saveAll(batchMetas)
    keyMetaService.saveAllComments(comments)
    keyMetaService.saveAllCodeReferences(codeReferences)

    // set links to comments and code references to point to correct (previous)
    // instances instead of the new empty ones
    comments.groupBy { it.keyMeta }.forEach { (keyMeta, c) ->
      keyMeta.comments = c.toMutableList()
    }
    codeReferences.groupBy { it.keyMeta }.forEach { (keyMeta, c) ->
      keyMeta.codeReferences = c.toMutableList()
    }
  }

  private fun getUnresolvedConflicts(conflicts: List<ImportTranslation>): List<SimpleImportConflictResult> {
    return conflicts.map {
      val conflict = it.conflict ?: throw IllegalStateException("Unresolved conflict should have conflict data")
      SimpleImportConflictResult(
        keyName = it.key.name,
        keyNamespace = conflict.key.namespace?.name,
        language = conflict.language.tag,
        isOverridable = ConflictType.isOverridable(it.conflictType),
      )
    }
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
    if (translationLanguageIds.isEmpty()) return
    securityService.checkLanguageTranslatePermission(import.project.id, translationLanguageIds.toList())
  }

  private fun checkKeyPermissions() {
    val isCreatingKey = keysToSaveValuesSequence().any { it.id == 0L }
    if (isCreatingKey) {
      securityService.checkProjectPermission(import.project.id, Scope.KEYS_CREATE)
    }
  }

  private fun handleKeyMetas() {
    importDataManager.storedKeysValuesSequence().forEach { importKey ->
      if (!importKey.shouldBeImported) {
        return@forEach
      }
      val ns = importKey.file.namespace
      val importedKeyMeta = storedMetas[ns to importKey.name]
      // don't touch key meta when imported key has no meta
      if (importedKeyMeta != null) {
        keysToSaveGet(ns, importKey.name)?.let { newKey ->
          // if key is obtained or created and meta exists, take it and import the data from the imported one
          // persist is cascaded on key, so it should be fine
          val keyMeta =
            importDataManager.existingMetas[ns to importKey.name]?.also {
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
    importDataManager.storedKeysValuesSequence().forEach { importKey ->
      if (!importKey.shouldBeImported) {
        return@forEach
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
      unresolvedConflicts.add(this)
      return
    }

    val language =
      this.language.existingLanguage
        ?: throw BadRequestException(Message.EXISTING_LANGUAGE_NOT_SELECTED)
    val translation =
      this.conflict ?: Translation().apply {
        this.language = language
      }

    translation.key = existingKey
    if (this.isBaseLanguageEditOfExisting(translation, language)) {
      outdatedFlagKeys.add(translation.key.id)
    }
    translationService.setTranslationTextNoSave(translation, text)
    translationsToSave.add(translation)
    translationLanguageIds.add(language.id)

    if (this.isPlural) {
      val ns = this.key.file.namespace
      val name = this.key.name
      val existingKey = importDataManager.existingKeys[ns to name]
      if (existingKey?.isPlural != true) {
        pluralFlipKeys[ns to name] = this.key.pluralArgName
      }
    }
  }

  private fun ImportTranslation.isBaseLanguageEditOfExisting(
    translation: Translation,
    language: Language,
  ): Boolean =
    this.conflict != null &&
      language == language.project.baseLanguage &&
      translation.text != this.text

  private val ImportTranslation.existingKey: Key
    get() {
      val ns = this.key.file.namespace
      val name = this.key.name
      // get key from already saved keys to save, or from conflict, or create new
      return keysToSaveGet(ns, name) ?: keysToSavePut(
        ns,
        name,
        importDataManager.existingKeys[ns to name] ?: createNewKey(name, ns),
      )
    }

  private fun addKeyToSave(
    namespace: String?,
    keyName: String,
  ): Key {
    return keysToSaveGet(namespace, keyName) ?: keysToSavePut(
      namespace,
      keyName,
      importDataManager.existingKeys[namespace to keyName] ?: createNewKey(keyName, namespace),
    )
  }

  private fun createNewKey(
    name: String,
    namespace: String?,
  ): Key {
    return Key(name = name).apply {
      project = import.project
      this.namespace = getNamespace(namespace)
      this.branch = import.branch
      newKeys.add(this)
    }
  }

  private fun ImportTranslation.getConflictResolution(): ForceMode {
    if (this.conflict == null) {
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
        ConflictType.isOverridable(this.conflictType)
      ) {
        return ForceMode.OVERRIDE
      }
      if (
        (overrideMode == OverrideMode.RECOMMENDED) &&
        ConflictType.isOverridableAndRecommended(this.conflictType)
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
}
