package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.dtos.dataImport.IImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.ImportFileProcessorFactory
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.model.key.KeyMeta
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.language.LanguageService
import io.tolgee.util.Logging
import io.tolgee.util.filterFiles
import io.tolgee.util.getOrThrowIfMoreThanOne
import io.tolgee.util.getSafeNamespace
import org.springframework.context.ApplicationContext

class CoreImportFilesProcessor(
  val applicationContext: ApplicationContext,
  val import: Import,
  val params: IImportAddFilesParams = ImportAddFilesParams(),
  val importSettings: IImportSettings,
  val projectIcuPlaceholdersEnabled: Boolean = true,
  // single step import doesn't save data
  val saveData: Boolean = true,
) : Logging {
  private val importService: ImportService by lazy { applicationContext.getBean(ImportService::class.java) }
  private val importFileProcessorFactory: ImportFileProcessorFactory by lazy {
    applicationContext.getBean(
      ImportFileProcessorFactory::class.java,
    )
  }
  private val tolgeeProperties: TolgeeProperties by lazy { applicationContext.getBean(TolgeeProperties::class.java) }
  private val languageService: LanguageService by lazy { applicationContext.getBean(LanguageService::class.java) }
  private val keyMetaService: KeyMetaService by lazy {
    applicationContext.getBean(KeyMetaService::class.java)
  }

  private val existingLanguages by lazy {
    languageService.getProjectLanguages(projectId = import.project.id)
  }

  val importDataManager by lazy {
    ImportDataManager(
      applicationContext = applicationContext,
      import = import,
      saveData = false,
    )
  }

  val errors = mutableListOf<ErrorResponseBody>()
  val warnings = mutableListOf<ErrorResponseBody>()

  fun processFiles(files: Collection<ImportFileDto>?) {
    errors.addAll(processFilesRecursive(files))
    renderPossibleNamespacesWarning()
  }

  private fun processFilesRecursive(files: Collection<ImportFileDto>?): List<ErrorResponseBody> {
    val errors = mutableListOf<ErrorResponseBody>()
    files?.forEach {
      try {
        val newErrors = processFileOrArchive(it)
        errors.addAll(newErrors)
      } catch (e: ImportCannotParseFileException) {
        errors.add(ErrorResponseBody(e.code, e.params))
      }
    }

    importDataManager.handleConflicts(false)
    return errors
  }

  private fun processFileOrArchive(file: ImportFileDto): List<ErrorResponseBody> {
    val errors = mutableListOf<ErrorResponseBody>()

    if (file.isArchive) {
      return processArchive(file, errors)
    }

    processFile(file)
    return listOf()
  }

  private fun processFile(file: ImportFileDto) {
    val savedFileEntity = file.saveFileEntity()
    val fileProcessorContext =
      FileProcessorContext(
        file = file,
        fileEntity = savedFileEntity,
        maxTranslationTextLength = tolgeeProperties.maxTranslationTextLength,
        params = params,
        importSettings,
        projectIcuPlaceholdersEnabled,
        applicationContext,
      )
    val processor = importFileProcessorFactory.getProcessor(file, fileProcessorContext)
    processor.process()
    processor.processResult()
    savedFileEntity.updateFileEntity(fileProcessorContext)
  }

  private fun renderPossibleNamespacesWarning() {
    if (import.project.useNamespaces) {
      return
    }

    val anyHasDetectedNamespace = import.files.any { it.detectedNamespace != null }
    val allLanguages = import.files.flatMap { file -> file.languages.map { language -> language.name } }
    val languageDuplicated = allLanguages.size != allLanguages.distinct().size

    if (!languageDuplicated && !anyHasDetectedNamespace) {
      return
    }

    warnings.add(ErrorResponseBody(Message.NAMESPACE_CANNOT_BE_USED_WHEN_FEATURE_IS_DISABLED.code, listOf()))
  }

  private fun ImportFile.updateFileEntity(fileProcessorContext: FileProcessorContext) {
    if (fileProcessorContext.needsParamConversion) {
      this.needsParamConversion = fileProcessorContext.needsParamConversion
      if (saveData) {
        importService.saveFile(this)
      }
    }
  }

  private fun processArchive(
    archive: ImportFileDto,
    errors: MutableList<ErrorResponseBody>,
  ): MutableList<ErrorResponseBody> {
    val processor = importFileProcessorFactory.getArchiveProcessor(archive)
    val files = processor.process(archive)
    val filtered = filterFiles(files.map { it.name to it })
    errors.addAll(processFilesRecursive(filtered))
    return errors
  }

  private val ImportFileDto.isArchive: Boolean
    get() {
      return this.name.endsWith(".zip")
    }

  private fun ImportFileDto.saveFileEntity(): ImportFile {
    val entity =
      ImportFile(
        name,
        import,
      )
    import.files.add(entity)
    if (saveData) {
      importService.saveFile(entity)
    }
    return entity
  }

  private fun ImportFileProcessor.processResult() {
    context.preselectNamespace()
    context.processLanguages()
    context.processTranslations()
    if (saveData) {
      importService.saveAllFileIssues(this.context.fileEntity.issues)
    }
  }

  private fun FileProcessorContext.preselectNamespace() {
    val namespace = getNamespaceToPreselect()
    if (namespace != null) {
      fileEntity.detectedNamespace = namespace
      if (import.project.useNamespaces) {
        fileEntity.namespace = namespace
      }
    }
  }

  private fun FileProcessorContext.getNamespaceToPreselect(): String? {
    val mappedNamespace = findMappedNamespace()

    if (mappedNamespace != null) {
      if (!import.project.useNamespaces && mappedNamespace.isNotEmpty()) {
        throw BadRequestException(Message.NAMESPACE_CANNOT_BE_USED_WHEN_FEATURE_IS_DISABLED)
      }
      return getSafeNamespace(mappedNamespace)
    }

    return getGuessedNamespace()
  }

  private fun FileProcessorContext.getGuessedNamespace(): String? {
    if (this.namespace != null) {
      // namespace was selected by processor
      return getSafeNamespace(this.namespace)
    }

    // select namespace from file name
    val namespace = getNamespaceFromPath()
    if (!namespace.isNullOrBlank()) {
      return namespace
    }

    return null
  }

  private fun FileProcessorContext.findMappedNamespace(): String? {
    if (mapping != null) {
      // if mapping is present, use it even when the namespace is null to avoid
      // guessing from the file name
      return this.mapping?.namespace ?: ""
    }

    return this.mapping?.namespace
  }

  private fun FileProcessorContext.getNamespaceFromPath() =
    """^[\/]?([^/\\]+)[/\\].*""".toRegex().matchEntire(this.fileEntity.name!!)?.groups?.get(1)?.value

  private fun FileProcessorContext.processLanguages() {
    this.languages.forEach { entry ->
      val languageEntity = entry.value
      importDataManager.storedLanguages.add(languageEntity)

      if (!shouldBeImported(languageEntity)) {
        languageEntity.ignored = true
        return@forEach
      }

      preselectExistingLanguage(languageEntity)
      if (saveData) {
        importService.saveLanguages(this.languages.values)
      }
      importDataManager.populateStoredTranslations(entry.value)
    }
  }

  private fun FileProcessorContext.shouldBeImported(languageEntity: ImportLanguage): Boolean {
    val languageTagsToImport = mapping?.languageTagsToImport
    return languageTagsToImport == null || languageTagsToImport.contains(languageEntity.name)
  }

  private fun FileProcessorContext.preselectExistingLanguage(languageEntity: ImportLanguage) {
    val existingLanguageDto = findExistingLanguage(languageEntity)
    languageEntity.existingLanguage = existingLanguageDto?.id?.let { languageService.getEntity(it) }
  }

  private fun FileProcessorContext.findExistingLanguage(languageEntity: ImportLanguage): LanguageDto? {
    return findExistingLanguageInMappings(languageEntity)
      ?: findMatchingExistingLanguage(languageEntity.name)
  }

  private fun findMatchingExistingLanguage(importLanguageName: String): LanguageDto? {
    val possibleTag =
      """(?:.*?)/?([a-zA-Z0-9-_]+)[^/]*?"""
        .toRegex()
        .matchEntire(importLanguageName)
        ?.groups
        ?.get(1)
        ?.value
        ?: return null

    val candidate = languageService.findByTag(possibleTag, import.project.id)

    return candidate
  }

  private fun FileProcessorContext.findExistingLanguageInMappings(languageEntity: ImportLanguage): LanguageDto? {
    val desiredTag = findInLanguageMappings(languageEntity) ?: findFileLanguageMapping() ?: return null
    return existingLanguages.find { it.tag == desiredTag }
  }

  private fun FileProcessorContext.findFileLanguageMapping(): String? {
    val mapping = mapping ?: return null
    return mapping.languageTag
  }

  private fun FileProcessorContext.findInLanguageMappings(languageEntity: ImportLanguage): String? {
    val languageMappings = singleStepImportParams?.languageMappings ?: return null
    val found =
      languageMappings
        .filter { it.importLanguage == languageEntity.name }
        .getOrThrowIfMoreThanOne {
          BadRequestException(Message.MULTIPLE_MAPPINGS_FOR_SAME_FILE_LANGUAGE_NAME)
        }

    return found?.platformLanguageTag
  }

  private fun addToStoredTranslations(translation: ImportTranslation) {
    importDataManager.storedTranslations[translation.language]!!.let { it[translation.key]!!.add(translation) }
  }

  fun saveKeyMeta(keyMeta: KeyMeta) {
    keyMeta.disableActivityLogging = true
    keyMetaService.save(keyMeta)
    keyMetaService.saveAllComments(keyMeta.comments)
    keyMetaService.saveAllCodeReferences(keyMeta.codeReferences)
  }

  private fun FileProcessorContext.getOrCreateKey(name: String): ImportKey {
    return importDataManager.storedKeys.computeIfAbsent(this.fileEntity to name) {
      this.keys
        .computeIfAbsent(name) {
          ImportKey(name = name, this.fileEntity)
        }.also {
          it.keyMeta?.also(importDataManager::prepareKeyMeta)
          if (saveData) {
            importService.saveKey(it)
            it.keyMeta?.also(this@CoreImportFilesProcessor::saveKeyMeta)
          }
        }
    }
  }

  private fun FileProcessorContext.processTranslations() {
    this.translations.forEach { entry ->
      val keyEntity = getOrCreateKey(entry.key)
      entry.value.forEach translationForeach@{ newTranslation ->
        processTranslation(newTranslation, keyEntity)
      }
      keyEntity.shouldBeImported = shouldImportKey(keyEntity.name)
    }
    if (saveData) {
      importDataManager.saveAllStoredTranslations()
    }
  }

  private fun FileProcessorContext.shouldImportKey(keyName: String): Boolean {
    if (importSettings.createNewKeys) {
      return true
    }
    return importDataManager.existingKeys[this.getNamespaceToPreselect() to keyName] != null
  }

  private fun FileProcessorContext.processTranslation(
    newTranslation: ImportTranslation,
    keyEntity: ImportKey,
  ) {
    newTranslation.key = keyEntity
    val (isCollision, fileCollisions) = checkForInFileCollisions(newTranslation)
    if (isCollision) {
      fileEntity.addIssues(fileCollisions)
      return
    }
    val otherFilesCollisions =
      importDataManager.checkForOtherFilesCollisions(newTranslation)
    if (otherFilesCollisions.isNotEmpty()) {
      fileEntity.addIssues(otherFilesCollisions)
      newTranslation.isSelectedToImport = false
    }
    this@CoreImportFilesProcessor.addToStoredTranslations(newTranslation)
  }

  private fun checkForInFileCollisions(
    newTranslation: ImportTranslation,
  ): Pair<Boolean, MutableList<Pair<FileIssueType, Map<FileIssueParamType, String>>>> {
    var isCollision = false
    val issues =
      mutableListOf<Pair<FileIssueType, Map<FileIssueParamType, String>>>()
    val storedTranslations =
      importDataManager
        .getStoredTranslations(newTranslation.key, newTranslation.language)
    if (storedTranslations.isNotEmpty()) {
      isCollision = true
      storedTranslations.forEach { collision ->
        if (newTranslation.text == collision.text) {
          return@forEach
        }
        issues.add(
          FileIssueType.MULTIPLE_VALUES_FOR_KEY_AND_LANGUAGE to
            mapOf(
              FileIssueParamType.KEY_ID to collision.key.id.toString(),
              FileIssueParamType.LANGUAGE_ID to collision.language.id.toString(),
              FileIssueParamType.KEY_NAME to collision.key.name,
              FileIssueParamType.LANGUAGE_NAME to collision.language.name,
            ),
        )
      }
    }
    return isCollision to issues
  }
}
