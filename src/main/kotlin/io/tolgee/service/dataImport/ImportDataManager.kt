package io.tolgee.service.dataImport

import io.tolgee.model.Translation
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.service.KeyMetaService
import io.tolgee.service.KeyService
import io.tolgee.service.TranslationService
import org.springframework.context.ApplicationContext

class ImportDataManager(
        private val applicationContext: ApplicationContext,
        private val import: Import
) {
    val importService: ImportService by lazy { applicationContext.getBean(ImportService::class.java) }

    val keyService: KeyService by lazy { applicationContext.getBean(KeyService::class.java) }

    private val keyMetaService: KeyMetaService by lazy {
        applicationContext.getBean(KeyMetaService::class.java)
    }

    val storedKeys by lazy {
        importService.findKeys(import).asSequence().map { it.name to it }.toMap(mutableMapOf())
    }

    val storedLanguages by lazy {
        importService.findLanguages(import).toMutableList()
    }

    val storedTranslations = mutableMapOf<ImportLanguage, MutableMap<ImportKey, MutableList<ImportTranslation>>>()

    private val existingTranslations = mutableMapOf<Long, MutableMap<String, Translation>>()

    val existingKeys: MutableMap<String, Key> by lazy {
        keyService.getAll(import.repository.id).asSequence().map { it.name!! to it }.toMap().toMutableMap()
    }

    private val translationService: TranslationService by lazy {
        applicationContext.getBean(TranslationService::class.java)
    }

    val storedMetas: MutableMap<String, KeyMeta> by lazy {
        keyMetaService.getWithFetchedData(this.import).asSequence().map { it.importKey!!.name!! to it }.toMap().toMutableMap()
    }

    /**
     * Returns list of translations provided for a language and a key.
     * It returns collection since translations could collide, when an user uploads multiple files with different values
     * for a key
     */
    fun getStoredTranslations(key: ImportKey, language: ImportLanguage): MutableList<ImportTranslation> {
        this.populateStoredTranslations(language)
        val languageData = this.storedTranslations[language]!!

        return languageData[key] ?: let {
            languageData[key] = mutableListOf()
            languageData[key]!!
        }
    }

    fun getStoredTranslations(language: ImportLanguage): List<ImportTranslation> {
        return this.populateStoredTranslations(language).flatMap { it.value }
    }

    fun populateStoredTranslations(language: ImportLanguage): MutableMap<ImportKey, MutableList<ImportTranslation>> {
        var languageData = this.storedTranslations[language]
        if (languageData != null) {
            return languageData //it is already there
        }

        languageData = mutableMapOf()
        storedTranslations[language] = languageData
        val translations = importService.findTranslations(import, language.id)
        translations.forEach { importTranslation ->
            val keyTranslations = languageData[importTranslation.key] ?: let {
                languageData[importTranslation.key] = mutableListOf()
                languageData[importTranslation.key]!!
            }
            keyTranslations.add(importTranslation)
        }
        return languageData
    }

    fun handleConflicts() {
        populateExistingTranslations()
        this.storedTranslations.asSequence().flatMap { it.value.values }.flatMap { it }.forEach { storedTranslation ->
            val existingLanguage = storedTranslation.language.existingLanguage
            if (existingLanguage != null) {
                val existingTranslation = existingTranslations[existingLanguage.id]
                        ?.let { it[storedTranslation.key.name] }
                if (existingTranslation != null) {
                    storedTranslation.conflict = existingTranslation
                } else {
                    storedTranslation.conflict = null
                }
            }
        }
    }

    private fun populateExistingTranslations() {
        this.storedLanguages.asSequence().map { it.existingLanguage }.toSet().forEach { language ->
            if (language != null && existingTranslations[language.id] == null) {
                existingTranslations[language.id] = mutableMapOf<String, Translation>().apply {
                    translationService.getAllByLanguageId(language.id)
                            .forEach { translation -> put(translation.key!!.name!!, translation) }
                }
            }
        }
    }

    fun saveAllStoredTranslations() {
        this.storedTranslations.values.asSequence().flatMap { it.values }.flatMap { it }.toList().let {
            importService.saveTranslations(it)
        }
    }

    fun saveAllStoredKeys() {
        this.importService.saveAllKeys(this.storedKeys.values)
    }
}
