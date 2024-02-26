package io.tolgee.service.dataImport

import io.tolgee.api.IImportSettings
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.dataImport.IImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.formats.ImportFileProcessor
import io.tolgee.formats.ImportFileProcessorFactory
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.util.Logging
import io.tolgee.util.filterFiles
import io.tolgee.util.getSafeNamespace
import org.springframework.context.ApplicationContext

class CoreImportFilesProcessor(
  val applicationContext: ApplicationContext,
  val import: Import,
  val params: IImportAddFilesParams = ImportAddFilesParams(),
  val importSettings: IImportSettings,
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

  val importDataManager by lazy {
    ImportDataManager(
      applicationContext = applicationContext,
      import = import,
      saveData = false,
    )
  }

  fun processFiles(files: Collection<ImportFileDto>?): MutableList<ErrorResponseBody> {
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

  private fun processFileOrArchive(file: ImportFileDto): MutableList<ErrorResponseBody> {
    val errors = mutableListOf<ErrorResponseBody>()

    if (file.isArchive) {
      return processArchive(file, errors)
    }

    processFile(file)
    return mutableListOf()
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
        applicationContext,
      )
    val processor = importFileProcessorFactory.getProcessor(file, fileProcessorContext)
    processor.process()
    processor.processResult()
    savedFileEntity.updateFileEntity(fileProcessorContext)
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
    errors.addAll(processFiles(filtered))
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
    if (this.namespace != null) {
      // namespace was selected by processor
      this.fileEntity.namespace = getSafeNamespace(this.namespace)
      return
    }
    // select namespace from file name
    val namespace = """^[\/]?([^/\\]+)[/\\].*""".toRegex().matchEntire(this.fileEntity.name!!)?.groups?.get(1)?.value
    if (!namespace.isNullOrBlank()) {
      this.fileEntity.namespace = namespace
    }
  }

  private fun FileProcessorContext.processLanguages() {
    this.languages.forEach { entry ->
      val languageEntity = entry.value
      importDataManager.storedLanguages.add(languageEntity)
      val existingLanguageDto = importDataManager.findMatchingExistingLanguage(languageEntity.name)
      languageEntity.existingLanguage = existingLanguageDto?.id?.let { languageService.getEntity(it) }
      if (saveData) {
        importService.saveLanguages(this.languages.values)
      }
      importDataManager.populateStoredTranslations(entry.value)
    }
  }

  private fun addToStoredTranslations(translation: ImportTranslation) {
    importDataManager.storedTranslations[translation.language]!!.let { it[translation.key]!!.add(translation) }
  }

  private fun FileProcessorContext.getOrCreateKey(name: String): ImportKey {
    return importDataManager.storedKeys.computeIfAbsent(this.fileEntity to name) {
      this.keys.computeIfAbsent(name) {
        ImportKey(name = name, this.fileEntity)
      }.also {
        if (saveData) {
          importService.saveKey(it)
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
    }
    importDataManager.prepareKeyMetas()
    if (saveData) {
      importDataManager.saveAllStoredKeys()
      importDataManager.saveAllKeyMetas()
      importDataManager.saveAllStoredTranslations()
    }
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
