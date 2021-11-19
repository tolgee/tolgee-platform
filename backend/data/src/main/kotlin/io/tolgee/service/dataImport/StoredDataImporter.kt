package io.tolgee.service.dataImport

import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.KeyMetaService
import io.tolgee.service.KeyService
import io.tolgee.service.TranslationService
import org.springframework.context.ApplicationContext

class StoredDataImporter(
  applicationContext: ApplicationContext,
  private val import: Import,
  private val forceMode: ForceMode = ForceMode.NO_FORCE
) {
  private val importDataManager = ImportDataManager(applicationContext, import)
  private val keyService = applicationContext.getBean(KeyService::class.java)
  private val keyMetaService = applicationContext.getBean(KeyMetaService::class.java)
  private val translationsToSave = mutableListOf<Translation>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction,
   * looking for key in this map is also faster then querying database
   */
  private val keysToSave = mutableMapOf<String, Key>()

  /**
   * We need to persist data after everything is checked for resolved conflicts since
   * thrown ImportConflictNotResolvedException commits the transaction
   */
  private val translationService = applicationContext.getBean(TranslationService::class.java)

  fun doImport() {
    importDataManager.storedLanguages.forEach {
      it.doImport()
    }

    this.importDataManager.storedKeys.values.onEach { importKey ->
      val importedKeyMeta = importDataManager.storedMetas[importKey.name]
      // dont touch key meta when imported key has no meta
      if (importedKeyMeta != null) {
        keysToSave[importKey.name]?.let { newKey ->
          // if key is obtained or created and meta exists, take it and import the data from the imported one
          // persist is cascaded on key, so it should be fine
          val keyMeta = importDataManager.existingMetas[importKey.name]?.also {
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
    keyService.saveAll(keysToSave.values)
    translationService.saveAll(translationsToSave)

    keysToSave.values.flatMap {
      it.keyMeta?.comments ?: emptyList()
    }.also { keyMetaService.saveAllComments(it) }

    keysToSave.values.flatMap {
      it.keyMeta?.codeReferences ?: emptyList()
    }.also { keyMetaService.saveAllCodeReferences(it) }
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
      translation.text = this@doImport.text
      translationsToSave.add(translation)
    }
  }

  private val ImportTranslation.existingKey: Key
    get() {
      // get key from already saved keys to save
      val key = keysToSave[this.key.name] ?: let {
        // or get it from conflict or create new one
        val newKey = this.conflict?.key ?: importDataManager.existingKeys[this.key.name]
          ?: Key(name = this.key.name).apply { project = import.project }
        newKey
      }
      val keyName = key.name
      keysToSave[keyName] = key
      return key
    }

  private fun ImportTranslation.checkConflictResolved() {
    if (forceMode == ForceMode.NO_FORCE && this.conflict != null && !this.resolved) {
      importDataManager.saveAllStoredTranslations()
      throw ImportConflictNotResolvedException(
        mutableListOf(this.key.name, this.language.name, this.text).filterNotNull().toMutableList()
      )
    }
  }
}
