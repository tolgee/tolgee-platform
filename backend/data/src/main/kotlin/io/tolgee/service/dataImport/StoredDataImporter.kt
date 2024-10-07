package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.enums.Scope
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.translation.Translation
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.TagService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.flushAndClear
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext

class StoredDataImporter(
  applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE,
  private val reportStatus: (ImportApplicationStatus) -> Unit = {},
  private val importSettings: IImportSettings,
  // for single step import, we provide import data manager
  private val _importDataManager: ImportDataManager? = null,
  private val isSingleStepImport: Boolean = false,
) {
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

  fun doImport() {
    reportStatus(ImportApplicationStatus.PREPARING_AND_VALIDATING)
    importDataManager.storedLanguages.forEach {
      it.prepareImport()
    }
    addKeysAndCheckPermissions()

    handleKeyMetas()

    reportStatus(ImportApplicationStatus.STORING_KEYS)

    namespaceService.saveAll(namespacesToSave.values)

    val keyEntitiesToSave = saveKeys()

    handlePluralization()

    saveKeyMetaData(keyEntitiesToSave)

    reportStatus(ImportApplicationStatus.STORING_TRANSLATIONS)

    saveTranslations()

    reportStatus(ImportApplicationStatus.FINALIZING)

    entityManager.flush()

    translationService.setOutdatedBatch(outdatedFlagKeys)

    tagNewKeys()

    entityManager.flush()

    deleteOtherKeys()

    entityManager.flushAndClear()
  }

  private fun tagNewKeys() {
    (importSettings as? SingleStepImportRequest)?.tagNewKeys?.let { tagNewKeys ->
      tagService.tagKeys(newKeys.associateWith { tagNewKeys })
    }
  }

  private fun deleteOtherKeys() {
    if ((importSettings as? SingleStepImportRequest)?.removeOtherKeys == true) {
      val existingKeys = importDataManager.existingKeys.entries
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
    keyMetaService.saveAll(keyMetasToSave)
    keyEntitiesToSave.flatMap {
      it.keyMeta?.comments ?: emptyList()
    }.also { keyMetaService.saveAllComments(it) }
    keyEntitiesToSave.flatMap {
      it.keyMeta?.codeReferences ?: emptyList()
    }.also { keyMetaService.saveAllCodeReferences(it) }
  }

  private fun saveTranslations() {
    checkTranslationPermissions()
    translationService.saveAll(translationsToSave.map { it.second })
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
    if (!this.isSelectedToImport || !this.key.shouldBeImported) {
      return
    }
    this.checkConflictResolved()
    if (this.conflict == null || (this.override && this.resolved) || forceMode == ForceMode.OVERRIDE) {
      val language =
        this.language.existingLanguage
          ?: throw BadRequestException(io.tolgee.constants.Message.EXISTING_LANGUAGE_NOT_SELECTED)
      val translation =
        this.conflict ?: Translation().apply {
          this.language = language
        }
      translation.key = existingKey
      if (language == language.project.baseLanguage && translation.text != this.text) {
        outdatedFlagKeys.add(translation.key.id)
      }
      translation.text = this@doImport.text
      translation.resetFlags()
      translationsToSave.add(this to translation)
    }
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

  private fun ImportTranslation.checkConflictResolved() {
    if (forceMode == ForceMode.NO_FORCE && this.conflict != null && !this.resolved) {
      if (importDataManager.saveData) {
        importDataManager.saveAllStoredTranslations()
      }
      throw ImportConflictNotResolvedException(
        mutableListOf(this.key.name, this.language.name, this.text).filterNotNull().toMutableList(),
      )
    }
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
