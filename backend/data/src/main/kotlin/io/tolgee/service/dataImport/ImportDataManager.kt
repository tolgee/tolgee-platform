package io.tolgee.service.dataImport

import io.tolgee.api.IStoredImportSettings
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

  /** Nested file → keyName → ImportKey. */
  val storedKeys: HashMap<ImportFile, HashMap<String, ImportKey>> by lazy {
    val result = HashMap<ImportFile, HashMap<String, ImportKey>>()
    if (!saveData) return@lazy result
    importService.findKeys(import).forEach {
      result.getOrPut(it.file) { HashMap() }[it.name] = it
    }
    result
  }

  fun storedKeysValuesSequence(): Sequence<ImportKey> =
    storedKeys.values.asSequence().flatMap { it.values.asSequence() }

  fun getStoredKey(
    file: ImportFile,
    name: String,
  ): ImportKey? = storedKeys[file]?.get(name)

  fun storedKeysComputeIfAbsent(
    file: ImportFile,
    name: String,
    create: () -> ImportKey,
  ): ImportKey = storedKeys.getOrPut(file) { HashMap() }.getOrPut(name, create)

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
              .getAllByLanguageId(language.id, import.branch?.name)
              .forEach { translation -> put(translation.key.namespace?.name to translation.key.name, translation) }
          }
      }
    }
    result
  }

  val existingKeys: MutableMap<Pair<String?, String>, Key> by lazy {
    keyService
      .getAllByBranch(import.project.id, import.branch?.name)
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

  fun releaseData() {
    storedKeys.clear()
    storedTranslations.clear()
    storedTranslationsByKeyName.clear()
    translationsToUpdateDueToCollisions.clear()
  }

  fun releaseLanguageData(language: ImportLanguage) {
    storedTranslations.remove(language)
    storedTranslationsByKeyName.remove(language)
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

    return languageData[key] ?: ArrayList<ImportTranslation>(1).also { languageData[key] = it }
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
    return otherLanguages.flatMap { language ->
      val cached = storedTranslationsByKeyName[language]
      if (cached != null) {
        cached[safeNamespace to keyName] ?: emptyList()
      } else {
        // Not cached — language is still being populated, use direct lookup
        val languageData = populateStoredTranslations(language)
        languageData.entries
          .filter { (key, _) -> key.name == keyName && key.file.namespace == safeNamespace }
          .flatMap { it.value }
      }
    }
  }

  /**
   * Cached map of stored translations grouped by (namespace, keyName) for O(1) lookup.
   * Only populated for languages whose translations are fully loaded.
   * Call [buildTranslationsByKeyNameCache] after a language is fully processed.
   */
  private val storedTranslationsByKeyName =
    mutableMapOf<ImportLanguage, Map<Pair<String?, String>, List<ImportTranslation>>>()

  fun buildTranslationsByKeyNameCache(language: ImportLanguage) {
    val languageData = populateStoredTranslations(language)
    val translationsByKey = HashMap<Pair<String?, String>, List<ImportTranslation>>(languageData.size)
    languageData.forEach { (key, translations) ->
      val mapKey = getSafeNamespace(key.file.namespace) to key.name
      val existing = translationsByKey[mapKey]
      translationsByKey[mapKey] =
        when {
          existing == null -> translations
          else -> existing + translations
        }
    }
    storedTranslationsByKeyName[language] = translationsByKey
  }

  fun populateStoredTranslations(language: ImportLanguage): MutableMap<ImportKey, MutableList<ImportTranslation>> {
    var languageData = this.storedTranslations[language]
    if (languageData != null) {
      return languageData // it is already there
    }

    languageData = LinkedHashMap()
    storedTranslations[language] = languageData
    val translations = importService.findTranslations(language.id)
    translations.forEach { importTranslation ->
      val keyTranslations =
        languageData.computeIfAbsent(importTranslation.key) { ArrayList(1) }
      keyTranslations.add(importTranslation)
    }
    return languageData
  }

  private fun getOrInitLanguageDataItem(
    language: ImportLanguage,
  ): MutableMap<ImportKey, MutableList<ImportTranslation>> {
    return this.storedTranslations.computeIfAbsent(language) { LinkedHashMap() }
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
    this.importService.saveAllKeys(storedKeysValuesSequence().toList())
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
    storedTranslationsByKeyName.clear()
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
    oldSettings: IStoredImportSettings,
    newSettings: IStoredImportSettings,
  ) {
    if (oldSettings.createNewKeys != newSettings.createNewKeys) {
      applyKeyCreateChange(newSettings.createNewKeys)
    }
  }

  fun applyKeyCreateChange(createNewKeys: Boolean) {
    storedKeysValuesSequence().forEach { key ->
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
}
