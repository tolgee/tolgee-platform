package io.tolgee.service.dataImport

import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.service.LanguageService
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.Logging
import org.springframework.context.ApplicationContext

class ImportDataManager(
  private val applicationContext: ApplicationContext,
  private val import: Import,
) : Logging {
  private val importService: ImportService by lazy { applicationContext.getBean(ImportService::class.java) }

  private val keyService: KeyService by lazy { applicationContext.getBean(KeyService::class.java) }

  private val namespaceService: NamespaceService by lazy { applicationContext.getBean(NamespaceService::class.java) }

  private val languageService: LanguageService by lazy { applicationContext.getBean(LanguageService::class.java) }

  private val keyMetaService: KeyMetaService by lazy {
    applicationContext.getBean(KeyMetaService::class.java)
  }

  val storedKeys by lazy {
    importService.findKeys(import)
      .asSequence()
      .map { (it.file to it.name) to it }
      .toMap(mutableMapOf())
  }

  val storedLanguages by lazy {
    importService.findLanguages(import).toMutableList()
  }

  val storedTranslations = mutableMapOf<ImportLanguage, MutableMap<ImportKey, MutableList<ImportTranslation>>>()

  /**
   * LanguageId to (Map of Pair(Namespace,KeyName) to Translation)
   */
  private val existingTranslations: MutableMap<Long, MutableMap<Pair<String?, String>, Translation>> by lazy {
    val result = mutableMapOf<Long, MutableMap<Pair<String?, String>, Translation>>()
    this.storedLanguages.asSequence().map { it.existingLanguage }.toSet().forEach { language ->
      if (language != null && result[language.id] == null) {
        result[language.id] =
          mutableMapOf<Pair<String?, String>, Translation>().apply {
            translationService.getAllByLanguageId(language.id)
              .forEach { translation -> put(translation.key.namespace?.name to translation.key.name, translation) }
          }
      }
    }
    result
  }

  val existingKeys: MutableMap<Pair<String?, String>, Key> by lazy {
    keyService.getAll(import.project.id)
      .asSequence()
      .map { (it.namespace?.name to it.name) to it }
      .toMap(mutableMapOf())
  }

  private val translationService: TranslationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }

  val existingMetas: MutableMap<Pair<String?, String>, KeyMeta> by lazy {
    keyMetaService.getWithFetchedData(this.import.project).asSequence()
      .map { (it.key!!.namespace?.name to it.key!!.name) to it }
      .toMap().toMutableMap()
  }

  val existingNamespaces by lazy {
    namespaceService.getAllInProject(import.project.id).map { it.name to it }.toMap(mutableMapOf())
  }

  /**
   * Returns list of translations provided for a language and a key.
   * It returns collection since translations could collide, when a user uploads multiple files with different values
   * for a key
   */
  fun getStoredTranslations(
    key: ImportKey,
    language: ImportLanguage,
  ): MutableList<ImportTranslation> {
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
      return languageData // it is already there
    }

    languageData = mutableMapOf()
    storedTranslations[language] = languageData
    val translations = importService.findTranslations(language.id)
    translations.forEach { importTranslation ->
      val keyTranslations =
        languageData[importTranslation.key] ?: let {
          languageData[importTranslation.key] = mutableListOf()
          languageData[importTranslation.key]!!
        }
      keyTranslations.add(importTranslation)
    }
    return languageData
  }

  /**
   * @param removeEqual Whether translations with equal texts should be removed
   */
  fun handleConflicts(removeEqual: Boolean) {
    this.storedTranslations.asSequence().flatMap { it.value.values }.forEach { languageTranslations ->
      val toRemove = mutableListOf<ImportTranslation>()
      languageTranslations.forEach { importedTranslation ->
        val existingLanguage = importedTranslation.language.existingLanguage
        if (existingLanguage != null) {
          val existingTranslation =
            existingTranslations[existingLanguage.id]
              ?.let { it[importedTranslation.language.file.namespace to importedTranslation.key.name] }
          if (existingTranslation != null) {
            // remove if text is the same
            if (existingTranslation.text == importedTranslation.text) {
              toRemove.add(importedTranslation)
            } else {
              importedTranslation.conflict = existingTranslation
            }
          } else {
            importedTranslation.conflict = null
          }
        }
      }
      if (removeEqual) {
        languageTranslations.removeAll(toRemove)
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
    saveAllMetas()
  }

  private fun saveAllMetas() {
    this.storedKeys.mapNotNull { it.value.keyMeta }.forEach { meta ->
      meta.disableActivityLogging = true
      keyMetaService.save(meta)
      meta.comments.onEach { comment -> comment.author = comment.author ?: import.author }
      keyMetaService.saveAllComments(meta.comments)
      meta.codeReferences.onEach { ref -> ref.author = ref.author ?: import.author }
      keyMetaService.saveAllCodeReferences(meta.codeReferences)
    }
  }

  private fun resetConflicts(importLanguage: ImportLanguage) {
    this.storedTranslations[importLanguage]?.values?.asSequence()?.flatMap { it }?.forEach {
      it.conflict = null
      it.resolvedHash = null
    }
  }

  fun resetLanguage(importLanguage: ImportLanguage) {
    this.populateStoredTranslations(importLanguage)
    this.resetConflicts(importLanguage)
    this.handleConflicts(false)
    if (isExistingLanguageUsed(importLanguage.existingLanguage?.id, importLanguage)) {
      importLanguage.existingLanguage = null
    }
    this.importService.saveLanguages(listOf(importLanguage))
    this.saveAllStoredTranslations()
  }

  private fun isExistingLanguageUsed(
    existingId: Long?,
    imported: ImportLanguage,
  ): Boolean {
    existingId ?: return false
    return this.storedLanguages.any { storedLang ->
      imported != storedLang && // ignore when is assigned to self
        storedLang.existingLanguage?.id == existingId &&
        storedLang.file.namespace == imported.file.namespace
    }
  }

  fun findMatchingExistingLanguage(importLanguage: ImportLanguage): LanguageDto? {
    val possibleTag =
      """(?:.*?)/?([a-zA-Z0-9-_]+)[^/]*?""".toRegex()
        .matchEntire(importLanguage.name)?.groups?.get(1)?.value
        ?: return null

    val candidate = languageService.findByTag(possibleTag, import.project.id)

    if (!isExistingLanguageUsed(candidate?.id, importLanguage)) {
      return candidate
    }

    return null
  }
}
