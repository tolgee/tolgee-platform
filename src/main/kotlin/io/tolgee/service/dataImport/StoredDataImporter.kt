package io.tolgee.service.dataImport

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.service.KeyService
import io.tolgee.service.TranslationService
import org.springframework.context.ApplicationContext

class StoredDataImporter(
        applicationContext: ApplicationContext,
        private val import: Import
) {
    private val importDataCache = ImportDataCache(applicationContext, import)
    private val translationService = applicationContext.getBean(TranslationService::class.java)
    private val keyService = applicationContext.getBean(KeyService::class.java)

    fun doImport() {
        importDataCache.storedLanguages.forEach {
            it.doImport()
        }
    }

    private fun ImportLanguage.doImport() {
        importDataCache.populateStoredTranslations(this)
        importDataCache.handleConflicts()
        importDataCache.getStoredTranslations(this).forEach { it.doImport() }
    }

    private fun ImportTranslation.doImport() {
        this.checkConflictResolved()
        if (this.conflict == null || this.override) {
            val key = keyService.getOrCreateKey(import.repository, this.key.name)
            val language = this.language.existingLanguage
                    ?: throw BadRequestException(Message.EXISTING_LANGUAGE_NOT_SELECTED)
            translationService.setTranslation(key, language, this.text)
        }
    }

    private fun ImportTranslation.checkConflictResolved() {
        if (this.conflict != null && !this.resolved) {
            throw BadRequestException(
                    Message.CONFLICT_IS_NOT_RESOLVED,
                    listOf(this.key.name, this.language.name, this.text)
            )
        }
    }
}
