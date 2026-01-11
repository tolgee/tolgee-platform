package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.formats.CollisionHandler
import io.tolgee.formats.isSamePossiblePlural
import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.enums.ConflictType
import io.tolgee.model.enums.TranslationProtection
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.security.ProjectHolder
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.security.SecurityService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.Logging
import io.tolgee.util.getSafeNamespace
import org.springframework.context.ApplicationContext

class ImportDataManager(
  private val applicationContext: ApplicationContext,
  private val import: Import,
  // data is not saved when single step import is used
  val saveData: Boolean = true,
) : Logging {
  private val importService: ImportService by lazy { applicationContext.getBean(ImportService::class.java) }

  private val keyService: KeyService by lazy { applicationContext.getBean(KeyService::class.java) }

  private val namespaceService: NamespaceService by lazy { applicationContext.getBean(NamespaceService::class.java) }

  private val projectHolder: ProjectHolder by lazy { applicationContext.getBean(ProjectHolder::class.java) }

  private val securityService: SecurityService by lazy { applicationContext.getBean(SecurityService::class.java) }

  private val collisionHandlers by lazy {
    applicationContext.getBeansOfType(CollisionHandler::class.java).values
  }

  private val keyMetaService: KeyMetaService by lazy {
    applicationContext.getBean(KeyMetaService::class.java)
  }

  val storedKeys by lazy {
    if (!saveData) {
      return@lazy mutableMapOf<Pair<ImportFile, String>, ImportKey>()
    }
    importService
      .findKeys(import)
      .asSequence()
      .map { (it.file to it.name) to it }
      .toMap(mutableMapOf())
  }

  val storedLanguages by lazy {
    importService.findLanguages(import).toMutableList()
  }

  val storedTranslations = mutableMapOf<ImportLanguage, MutableMap<ImportKey, MutableList<ImportTranslation>>>()

  val translationsToUpdateDueToCollisions = mutableListOf<ImportTranslation>()

  /**
   * LanguageId to (Map of Pair(Namespace,KeyName) to Translation)
   */
  private val existingTranslations: MutableMap<Long, MutableMap<Pair<String?, String>, Translation>> by lazy {
    val result = mutableMapOf<Long, MutableMap<Pair<String?, String>, Translation>>()
    this.storedLanguages.asSequence().map { it.existingLanguage }.toSet().forEach { language ->
      if (language != null && result[language.id] == null) {
        result[language.id] =
          mutableMapOf<Pair<String?, String>, Translation>().apply {
            translationService
              .getAllByLanguageId(language.id)
              .forEach { translation -> put(translation.key.namespace?.name to translation.key.name, translation) }
          }
      }
    }
    result
  }

  val existingKeys: MutableMap<Pair<String?, String>, Key> by lazy {
    keyService
      .getAll(import.project.id)
      .asSequence()
      .map { (it.namespace?.name to it.name) to it }
      .toMap(mutableMapOf())
  }

  private val translationService: TranslationService by lazy {
    applicationContext.getBean(TranslationService::class.java)
  }

  val existingMetas: MutableMap<Pair<String?, String>, KeyMeta> by lazy {
    keyMetaService
      .getWithFetchedData(this.import.project)
      .asSequence()
      .map { (it.key!!.namespace?.name to it.key!!.name) to it }
      .toMap()
      .toMutableMap()
  }

  val existingNamespaces by lazy {
    namespaceService.getAllInProject(import.project.id).map { it.name to it }.toMap(mutableMapOf())
  }

  /**
   * Returns list of translations provided for a language and a key.
   * It returns collection since translations could collide, when a user uploads a file with different values
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

  private fun getStoredTranslations(
    keyName: String,
    keyNamespace: String?,
    otherLanguages: List<ImportLanguage>,
  ): List<ImportTranslation> {
    val safeNamespace = getSafeNamespace(keyNamespace)
    return otherLanguages.flatMap {
      getStoredTranslations(it).filter { translation ->
        translation.key.name == keyName && translation.key.file.namespace == safeNamespace
      }
    }
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
        languageData.computeIfAbsent(importTranslation.key) { mutableListOf() }
      keyTranslations.add(importTranslation)
    }
    return languageData
  }

  private fun populateStoredTranslationsToConvertPlaceholders() {
    val translations = importService.findTranslationsForPlaceholderConversion(import.id)
    translations.forEach {
      getOrInitLanguageDataItem(it.language)[it.key] = mutableListOf(it)
    }
  }

  private fun getOrInitLanguageDataItem(
    language: ImportLanguage,
  ): MutableMap<ImportKey, MutableList<ImportTranslation>> {
    return this.storedTranslations.computeIfAbsent(language) { mutableMapOf() }
  }

  private fun detectConflictType(translation: Translation): ConflictType? {
    if (translation.state === TranslationState.DISABLED) {
      return ConflictType.CANNOT_EDIT_DISABLED
    }

    if (
      translation.state == TranslationState.REVIEWED &&
      projectHolder.project.translationProtection == TranslationProtection.PROTECT_REVIEWED
    ) {
      if (
        !securityService.canEditReviewedTranslation(projectHolder.project.id, translation.language.id)
      ) {
        return ConflictType.CANNOT_EDIT_REVIEWED
      } else {
        return ConflictType.SHOULD_NOT_EDIT_REVIEWED
      }
    }
    return null
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
            if (existingTranslation.text isSamePossiblePlural importedTranslation.text) {
              toRemove.add(importedTranslation)
            } else {
              importedTranslation.conflict = existingTranslation
              importedTranslation.conflictType = detectConflictType(existingTranslation)
            }
          } else {
            importedTranslation.conflict = null
            importedTranslation.conflictType = null
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
  }

  fun prepareKeyMeta(keyMeta: KeyMeta) {
    keyMeta.comments.onEach { comment -> comment.author = comment.author ?: import.author }
    keyMeta.codeReferences.onEach { ref -> ref.author = ref.author ?: import.author }
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
    this.importService.saveLanguages(listOf(importLanguage))
    this.saveAllStoredTranslations()
  }

  fun resetCollisionsBetweenFiles(
    editedLanguage: ImportLanguage,
    oldExistingLanguage: Language? = null,
  ) {
    val affectedLanguages =
      storedLanguages
        .filter {
          (
            (editedLanguage.existingLanguage == it.existingLanguage && it.existingLanguage != null) ||
              (oldExistingLanguage == it.existingLanguage && it.existingLanguage != null)
          ) &&
            it != editedLanguage
        }.sortedBy { it.id } + listOf(editedLanguage)
    val affectedFiles = affectedLanguages.map { it.file }
    resetBetweenFileCollisionIssuesForFiles(affectedFiles.map { it.id }, affectedLanguages.map { it.id })
    val handledLanguages = mutableListOf<ImportLanguage>()
    val issuesToSave = mutableListOf<ImportFileIssue>()
    affectedLanguages.forEach { language ->
      getStoredTranslations(language).forEach {
        if (!it.isSelectedToImport) {
          translationsToUpdateDueToCollisions.add(it)
        }
        it.isSelectedToImport = true
      }

      if (handledLanguages.isEmpty()) {
        handledLanguages.add(language)
        resetIsSelected(language)
        return@forEach
      }

      getStoredTranslations(language).forEach { translation ->
        val withSameExistingLanguage =
          handledLanguages.filter { it.existingLanguage == language.existingLanguage && it.existingLanguage != null }
        val fileCollisions = checkForOtherFilesCollisions(translation, withSameExistingLanguage)
        translationsToUpdateDueToCollisions.add(translation)
        if (fileCollisions.isNotEmpty()) {
          translation.isSelectedToImport = false
          fileCollisions.forEach { (type, params) ->
            val issue = language.file.prepareIssue(type, params)
            issuesToSave.add(issue)
          }
        }
      }
    }

    updateTranslationsDueToCollisions()
    importService.saveAllFileIssues(issuesToSave)
  }

  private fun updateTranslationsDueToCollisions() {
    importService.updateIsSelectedForTranslations(translationsToUpdateDueToCollisions)
    translationsToUpdateDueToCollisions.clear()
  }

  private fun resetIsSelected(language: ImportLanguage) {
    getStoredTranslations(language).forEach { it.isSelectedToImport = true }
  }

  private fun getLanguagesWithSameExisting(importLanguage: ImportLanguage): List<ImportLanguage> {
    if (importLanguage.existingLanguage == null) {
      return emptyList()
    }
    return storedLanguages.filter { it.existingLanguage == importLanguage.existingLanguage }
  }

  private fun checkForOtherFilesCollisions(
    newTranslation: ImportTranslation,
    otherLanguages: List<ImportLanguage>,
  ): MutableList<Pair<FileIssueType, Map<FileIssueParamType, String>>> {
    val issues =
      mutableListOf<Pair<FileIssueType, Map<FileIssueParamType, String>>>()
    val storedTranslations =
      getStoredTranslations(
        newTranslation.key.name,
        newTranslation.key.file.namespace,
        otherLanguages,
      )

    storedTranslations
      .firstOrNull {
        it.isSelectedToImport
      }?.let { collision ->
        val handled = tryHandleUsingCollisionHandlers(listOf(newTranslation) + storedTranslations)
        if (handled) {
          return issues
        }
        issues.add(
          FileIssueType.TRANSLATION_DEFINED_IN_ANOTHER_FILE to
            mapOf(
              FileIssueParamType.KEY_ID to collision.key.id.toString(),
              FileIssueParamType.LANGUAGE_ID to collision.language.id.toString(),
              FileIssueParamType.KEY_NAME to collision.key.name,
              FileIssueParamType.LANGUAGE_NAME to collision.language.name,
            ),
        )
      }
    return issues
  }

  private fun tryHandleUsingCollisionHandlers(importTranslations: List<ImportTranslation>): Boolean {
    val toIgnore =
      collisionHandlers.firstNotNullOfOrNull {
        it.handle(importTranslations)
      } ?: return false

    toIgnore.forEach {
      translationsToUpdateDueToCollisions.add(it)
      it.isSelectedToImport = false
    }

    return true
  }

  private fun resetBetweenFileCollisionIssuesForFiles(
    fileIds: List<Long>,
    languageIds: List<Long>,
  ) {
    importService.deleteAllBetweenFileCollisionsForFiles(fileIds, languageIds)
  }

  fun checkForOtherFilesCollisions(
    newTranslation: ImportTranslation,
  ): MutableList<Pair<FileIssueType, Map<FileIssueParamType, String>>> {
    return checkForOtherFilesCollisions(newTranslation, getLanguagesWithSameExisting(newTranslation.language))
  }

  fun applySettings(
    oldSettings: IImportSettings,
    newSettings: IImportSettings,
  ) {
    if (oldSettings.convertPlaceholdersToIcu != newSettings.convertPlaceholdersToIcu) {
      applyConvertPlaceholdersChange(newSettings.convertPlaceholdersToIcu)
    }

    if (oldSettings.createNewKeys != newSettings.createNewKeys) {
      applyKeyCreateChange(newSettings.createNewKeys)
    }
  }

  fun applyKeyCreateChange(createNewKeys: Boolean) {
    storedKeys.forEach { (_, key) ->
      if (createNewKeys) {
        key.shouldBeImported = true
      } else {
        key.shouldBeImported = existingKeys[getSafeNamespace(key.file.namespace) to key.name] != null
      }
    }
    if (saveData) {
      saveAllStoredKeys()
    }
  }

  private fun applyConvertPlaceholdersChange(convertPlaceholdersToIcu: Boolean) {
    this.populateStoredTranslationsToConvertPlaceholders()
    val toSave = mutableListOf<ImportTranslation>()
    storedTranslations.forEach { (language, keyTranslationsMap) ->
      keyTranslationsMap.forEach { (_, translations) ->
        translations.forEach {
          val convertor = it.convertor?.messageConvertorOrNull
          if (convertor != null) {
            val prev = it.text to it.isPlural
            val converted =
              convertor.convert(
                rawData = it.rawData,
                languageTag = language.name,
                convertPlaceholders = convertPlaceholdersToIcu,
                isProjectIcuEnabled = import.project.icuPlaceholders,
              )
            it.isPlural = converted.pluralArgName != null
            it.text = converted.message
            val new = it.text to it.isPlural
            if (prev != new) {
              toSave.add(it)
            }
          }
        }
      }
    }
    toSave.map { it.language }.toSet().forEach {
      resetConflicts(it)
      handleConflicts(false)
    }
    importService.saveTranslations(toSave)
  }
}
