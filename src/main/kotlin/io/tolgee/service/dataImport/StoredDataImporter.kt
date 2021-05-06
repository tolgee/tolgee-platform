package io.tolgee.service.dataImport

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.model.Key
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
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
    private val translationsToSave = mutableListOf<Translation>()
    private val keysToSave = mutableListOf<Key>()
    private val translationService = applicationContext.getBean(TranslationService::class.java)

    fun doImport() {
        importDataManager.storedLanguages.forEach {
            it.doImport()
        }
        translationService.saveAll(translationsToSave)
        keyService.saveAll(keysToSave)
    }

    private fun ImportLanguage.doImport() {
        importDataManager.populateStoredTranslations(this)
        importDataManager.handleConflicts()
        importDataManager.getStoredTranslations(this).forEach { it.doImport() }
    }

    private fun ImportTranslation.doImport() {
        this.checkConflictResolved()
        if (this.conflict == null || (this.override && this.resolved) || forceMode == ForceMode.OVERRIDE) {
            val key = this.conflict?.key ?: keyService.getOrCreateKeyNoPersist(import.repository, this.key.name)
            keysToSave.add(key)
            val language = this.language.existingLanguage
                    ?: throw BadRequestException(Message.EXISTING_LANGUAGE_NOT_SELECTED)
            val translation = this.conflict ?: Translation().apply {
                this.language = language
                this.key = key
            }
            translation.text = this@doImport.text
            translationsToSave.add(translation)
        }
    }

    private fun ImportTranslation.checkConflictResolved() {
        if (forceMode == ForceMode.NO_FORCE && this.conflict != null && !this.resolved) {
            importDataManager.saveAllStoredTranslations()
            throw ImportConflictNotResolvedException(
                    mutableListOf(this.key.name, this.language.name, this.text)
            )
        }
    }
}
