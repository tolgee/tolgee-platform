package io.tolgee.service.dataImport

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
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import org.springframework.context.ApplicationContext

class StoredDataImporter(
  applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE,
) {

  private val importDataManager = ImportDataManager(applicationContext, import)
  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val namespaceService = applicationContext.getBean(NamespaceService::class.java)

  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)

  private val securityService = applicationContext.getBean(SecurityService::class.java)

  private val translationsToSave = mutableListOf<Translation>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction,
   * looking for key in this map is also faster than querying database
   */
  private val keysToSave = mutableMapOf<Pair<String?, String>, Key>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction
   */
  private val translationService = applicationContext.getBean(TranslationService::class.java)

  private val namespacesToSave = mutableMapOf<String?, Namespace>()

  /**
   * Keys where base translation was changed, so we need to set outdated flag on all translations
   */
  val outdatedFlagKeys: MutableList<Long> = mutableListOf()

  /**
   * This metas are merged, so there is only one meta for one key!!!
   *
   * It can be used only when we are finally importing the data, before that we cannot merge it,
   * since namespace can be changed
   */
  private val storedMetas: MutableMap<Pair<String?, String>, KeyMeta> by lazy {
    val result: MutableMap<Pair<String?, String>, KeyMeta> = mutableMapOf()
    keyMetaService.getWithFetchedData(this.import).forEach { currentKeyMeta ->
      val mapKey = currentKeyMeta.importKey!!.file.namespace to currentKeyMeta.importKey!!.name
      result[mapKey] = result[mapKey]?.let { existingKeyMeta ->
        keyMetaService.import(existingKeyMeta, currentKeyMeta)
        existingKeyMeta
      } ?: currentKeyMeta
    }
    result
  }

  fun doImport() {
    importDataManager.storedLanguages.forEach {
      it.doImport()
    }

    addKeysAndCheckPermissions()

    handleKeyMetas()

    namespaceService.saveAll(namespacesToSave.values)

    val keyEntitiesToSave = saveKeys()

    saveTranslations()

    saveMetaData(keyEntitiesToSave)

    translationService.setOutdatedBatch(outdatedFlagKeys)
  }

  private fun saveMetaData(keyEntitiesToSave: MutableCollection<Key>) {
    keyEntitiesToSave.flatMap {
      it.keyMeta?.comments ?: emptyList()
    }.also { keyMetaService.saveAllComments(it) }
    keyEntitiesToSave.flatMap {
      it.keyMeta?.codeReferences ?: emptyList()
    }.also { keyMetaService.saveAllCodeReferences(it) }
  }

  private fun saveTranslations() {
    checkTranslationPermissions()
    translationService.saveAll(translationsToSave)
  }

  private fun saveKeys(): MutableCollection<Key> {
    val keyEntitiesToSave = keysToSave.values
    keyService.saveAll(keyEntitiesToSave)
    return keyEntitiesToSave
  }

  private fun addKeysAndCheckPermissions() {
    addAllKeys()
    checkKeyPermissions()
  }

  private fun checkTranslationPermissions() {
    val langs = translationsToSave.map { it.language }.toSet().map { it.id }
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
      val importedKeyMeta = storedMetas[fileNamePair.first.namespace to importKey.name]
      // don't touch key meta when imported key has no meta
      if (importedKeyMeta != null) {
        keysToSave[fileNamePair.first.namespace to importKey.name]?.let { newKey ->
          // if key is obtained or created and meta exists, take it and import the data from the imported one
          // persist is cascaded on key, so it should be fine
          val keyMeta = importDataManager.existingMetas[fileNamePair.first.namespace to importKey.name]?.also {
            keyMetaService.import(it, importedKeyMeta)
          } ?: importedKeyMeta
          // also set key and remove import key
          keyMeta.also {
            it.key = newKey
            it.importKey = null
          }
          // assign new meta
          newKey.keyMeta = keyMeta
        }
      }
    }
  }

  private fun addAllKeys() {
    importDataManager.storedKeys.map { (fileNamePair, importKey) ->
      addKeyToSave(importKey.file.namespace, importKey.name)
    }
  }

  private fun ImportLanguage.doImport() {
    importDataManager.populateStoredTranslations(this)
    importDataManager.handleConflicts(true)
    importDataManager.getStoredTranslations(this).forEach { it.doImport() }
  }

  private fun ImportTranslation.doImport() {
    this.checkConflictResolved()
    if (this.conflict == null || (this.override && this.resolved) || forceMode == ForceMode.OVERRIDE) {
      val language = this.language.existingLanguage
        ?: throw BadRequestException(io.tolgee.constants.Message.EXISTING_LANGUAGE_NOT_SELECTED)
      val translation = this.conflict ?: Translation().apply {
        this.language = language
      }
      translation.key = existingKey
      if (language == language.project.baseLanguage && translation.text != this.text) {
        outdatedFlagKeys.add(translation.key.id)
      }
      translation.text = this@doImport.text
      translation.resetFlags()
      translationsToSave.add(translation)
    }
  }

  private val ImportTranslation.existingKey: Key
    get() {
      // get key from already saved keys to save
      return keysToSave.computeIfAbsent(this.key.file.namespace to this.key.name) {
        // or get it from conflict or create new one
        val newKey = this.conflict?.key
          ?: importDataManager.existingKeys[this.key.file.namespace to this.key.name]
          ?: Key(name = this.key.name).apply {
            project = import.project
            namespace = getNamespace(this@existingKey.key.file.namespace)
          }
        newKey
      }
    }

  private fun addKeyToSave(namespace: String?, keyName: String): Key {
    return keysToSave.computeIfAbsent(namespace to keyName) {
      importDataManager.existingKeys[namespace to keyName] ?: Key(name = keyName).apply {
        project = import.project
        this.namespace = getNamespace(namespace)
      }
    }
  }

  private fun ImportTranslation.checkConflictResolved() {
    if (forceMode == ForceMode.NO_FORCE && this.conflict != null && !this.resolved) {
      importDataManager.saveAllStoredTranslations()
      throw ImportConflictNotResolvedException(
        mutableListOf(this.key.name, this.language.name, this.text).filterNotNull().toMutableList()
      )
    }
  }

  private fun getNamespace(name: String?): Namespace? {
    name ?: return null
    return importDataManager.existingNamespaces[name] ?: namespacesToSave.computeIfAbsent(name) {
      Namespace(name, import.project)
    }
  }
}
