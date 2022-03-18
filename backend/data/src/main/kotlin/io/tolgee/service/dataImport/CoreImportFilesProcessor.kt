package io.tolgee.service.dataImport

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_FILES_IN_ARCHIVE
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType.FOUND_LANGUAGE
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ImportCannotParseFileException
import io.tolgee.model.Language
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.issueTypes.FileIssueType
import io.tolgee.model.dataImport.issues.paramTypes.FileIssueParamType
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.KeyMetaService
import io.tolgee.service.LanguageService
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.ProcessorFactory
import org.springframework.context.ApplicationContext

class CoreImportFilesProcessor(
  val applicationContext: ApplicationContext,
  val import: Import
) {
  private val importService: ImportService by lazy { applicationContext.getBean(ImportService::class.java) }
  private val keyMetaService: KeyMetaService by lazy { applicationContext.getBean(KeyMetaService::class.java) }
  private val languageService: LanguageService by lazy { applicationContext.getBean(LanguageService::class.java) }
  private val processorFactory: ProcessorFactory by lazy { applicationContext.getBean(ProcessorFactory::class.java) }
  private val tolgeeProperties: TolgeeProperties by lazy { applicationContext.getBean(TolgeeProperties::class.java) }

  private val authenticationFacade: AuthenticationFacade by lazy {
    applicationContext.getBean(AuthenticationFacade::class.java)
  }

  private val importDataManager = ImportDataManager(applicationContext, import)

  fun processFiles(
    files: List<ImportFileDto>?,
    messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit
  ): MutableList<ErrorResponseBody> {

    val errors = mutableListOf<ErrorResponseBody>()
    files?.forEach {
      try {
        errors.addAll(processFileOrArchive(it, messageClient))
      } catch (e: ImportCannotParseFileException) {
        errors.add(ErrorResponseBody(e.code, e.params))
      }
    }
    return errors
  }

  private fun processFileOrArchive(
    file: ImportFileDto,
    messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit
  ): MutableList<ErrorResponseBody> {
    val errors = mutableListOf<ErrorResponseBody>()

    if (file.isArchive) {
      messageClient(FOUND_ARCHIVE, null)
      val processor = processorFactory.getArchiveProcessor(file)
      processor.process(file).apply {
        messageClient(FOUND_FILES_IN_ARCHIVE, listOf(size))
        errors.addAll(processFiles(this, messageClient))
      }
      return errors
    }

    val savedFileEntity = file.saveFileEntity()
    val fileProcessorContext = FileProcessorContext(
      file,
      savedFileEntity,
      messageClient,
      tolgeeProperties.maxTranslationTextLength
    )
    val processor = processorFactory.getProcessor(file, fileProcessorContext)
    processor.process()
    processor.context.processResult()
    return errors
  }

  private val ImportFileDto.isArchive: Boolean
    get() {
      return this.name.endsWith(".zip")
    }

  private fun ImportFileDto.saveFileEntity() = importService.saveFile(ImportFile(this.name, import))

  private fun FileProcessorContext.processResult() {
    this.processLanguages()

    importDataManager.storedKeys // populate keys first
    this.mergeKeyMetas()
    importDataManager.storedMetas.values.forEach {
      it.let { meta ->
        if (meta.id == 0L) {
          keyMetaService.save(meta)
        }
        meta.comments.onEach { comment -> comment.author = comment.author ?: authenticationFacade.userAccountEntity }
        keyMetaService.saveAllComments(meta.comments)
        meta.codeReferences.onEach { ref -> ref.author = ref.author ?: authenticationFacade.userAccountEntity }
        keyMetaService.saveAllCodeReferences(meta.codeReferences)
      }
    }
    importDataManager.saveAllStoredKeys()
    this.processTranslations()

    importService.saveAllFileIssues(this.fileEntity.issues)
    importService.saveAllFileIssueParams(this.fileEntity.issues.flatMap { it.params ?: emptyList() })
  }

  private fun FileProcessorContext.processLanguages() {
    this.languages.forEach { entry ->
      val languageEntity = entry.value
      messageClient(FOUND_LANGUAGE, listOf(languageEntity.name))
      val matchingStoredLanguage = importDataManager.storedLanguages.find {
        it.name == entry.value.name && it.existingLanguage != null
      }
      if (matchingStoredLanguage == null) {
        languageEntity.existingLanguage = languageEntity.findMatchingExisting()
      }
      importService.saveLanguages(this.languages.values)
      importDataManager.storedLanguages.addAll(this.languages.values)
      importDataManager.populateStoredTranslations(entry.value)
    }
  }

  private fun addToStoredTranslations(translation: ImportTranslation) {
    importDataManager.storedTranslations[translation.language]!!.let { it[translation.key]!!.add(translation) }
  }

  private fun FileProcessorContext.getOrCreateKey(name: String): ImportKey {
    var entity = importDataManager.storedKeys[name]
    if (entity == null) {
      entity = this.keys[name] ?: ImportKey(name = name)
      importDataManager.storedKeys[name] = entity
    }

    if (!entity.files.any { this.fileEntity == it }) {
      entity.files.add(fileEntity)
      fileEntity.keys.add(entity)
      importService.saveKey(entity)
    }

    return entity
  }

  private fun ImportLanguage.findMatchingExisting(): Language? {
    return languageService.findByTag(this.name, import.project.id).orElse(null)
  }

  private fun FileProcessorContext.processTranslations() {
    this.translations.forEach { entry ->
      val keyEntity = getOrCreateKey(entry.key)
      entry.value.forEach { newTranslation ->
        val storedTranslations = importDataManager.getStoredTranslations(keyEntity, newTranslation.language)
        newTranslation.key = keyEntity
        if (storedTranslations.size > 1) {
          storedTranslations.forEach { collidingTranslations ->
            fileEntity.addIssue(
              FileIssueType.MULTIPLE_VALUES_FOR_KEY_AND_LANGUAGE,
              mapOf(
                FileIssueParamType.KEY_ID to collidingTranslations.key.id.toString(),
                FileIssueParamType.LANGUAGE_ID to collidingTranslations.language.id.toString()
              )
            )
          }
          return
        }
        this@CoreImportFilesProcessor.addToStoredTranslations(newTranslation)
      }
    }

    importDataManager.saveAllStoredKeys()
    importDataManager.handleConflicts(false)
    importDataManager.saveAllStoredTranslations()
  }

  private fun FileProcessorContext.mergeKeyMetas() {
    val storedMetas = importDataManager.storedMetas
    this.keys.values.forEach { newKey ->
      val storedMeta = storedMetas[newKey.name]
      val newMeta = newKey.keyMeta
      if (storedMeta != null && newMeta != null) {
        keyMetaService.import(storedMeta, newMeta)
      }
      if (storedMeta == null && newMeta != null) {
        storedMetas[newKey.name] = newMeta
      }
    }
  }
}
