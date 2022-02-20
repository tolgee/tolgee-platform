package io.tolgee.service.dataImport

import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.repository.dataImport.ImportFileRepository
import io.tolgee.repository.dataImport.ImportKeyRepository
import io.tolgee.repository.dataImport.ImportLanguageRepository
import io.tolgee.repository.dataImport.ImportRepository
import io.tolgee.repository.dataImport.ImportTranslationRepository
import io.tolgee.repository.dataImport.issues.ImportFileIssueParamRepository
import io.tolgee.repository.dataImport.issues.ImportFileIssueRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.project_auth.ProjectHolder
import io.tolgee.service.KeyMetaService
import org.apache.commons.lang3.time.DateUtils
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionInterceptor

@Service
@Transactional
class ImportService(
  private val importRepository: ImportRepository,
  private val importFileRepository: ImportFileRepository,
  private val authenticationFacade: AuthenticationFacade,
  private val projectHolder: ProjectHolder,
  private val importFileIssueRepository: ImportFileIssueRepository,
  private val importLanguageRepository: ImportLanguageRepository,
  private val importKeyRepository: ImportKeyRepository,
  private val applicationContext: ApplicationContext,
  private val importTranslationRepository: ImportTranslationRepository,
  private val importFileIssueParamRepository: ImportFileIssueParamRepository,
  private val keyMetaService: KeyMetaService,
  private val currentDateProvider: CurrentDateProvider
) {
  @Transactional
  fun addFiles(
    files: List<ImportFileDto>,
    messageClient: ((ImportStreamingProgressMessageType, List<Any>?) -> Unit)? = null,
    project: Project,
    userAccount: UserAccount
  ): List<ErrorResponseBody> {
    val import = find(project.id, userAccount.id)
      .removeIfExpired()
      ?: Import(userAccount, project)

    val nonNullMessageClient = messageClient ?: { _, _ -> }
    val languages = findLanguages(import)

    if (languages.count() + files.size > 100) {
      throw BadRequestException(io.tolgee.constants.Message.CANNOT_ADD_MORE_THEN_100_LANGUAGES)
    }

    importRepository.save(import)
    val fileProcessor = CoreImportFilesProcessor(
      applicationContext = applicationContext,
      import = import
    )
    val errors = fileProcessor.processFiles(files, nonNullMessageClient)

    if (findLanguages(import).isEmpty()) {
      TransactionInterceptor.currentTransactionStatus().setRollbackOnly()
    }
    return errors
  }

  @Transactional(noRollbackFor = [ImportConflictNotResolvedException::class])
  fun import(projectId: Long, authorId: Long, forceMode: ForceMode = ForceMode.NO_FORCE) {
    import(getNotExpired(projectId, authorId), forceMode)
  }

  @Transactional(noRollbackFor = [ImportConflictNotResolvedException::class])
  fun import(import: Import, forceMode: ForceMode = ForceMode.NO_FORCE) {
    StoredDataImporter(applicationContext, import, forceMode).doImport()
    deleteImport(import)
  }

  @Transactional
  fun selectExistingLanguage(importLanguage: ImportLanguage, existingLanguage: Language?) {
    val import = importLanguage.file.import
    val dataManager = ImportDataManager(applicationContext, import)
    existingLanguage?.let {
      if (dataManager.storedLanguages.any { it.existingLanguage?.id == existingLanguage.id }) {
        throw BadRequestException(io.tolgee.constants.Message.LANGUAGE_ALREADY_SELECTED)
      }
    }
    importLanguage.existingLanguage = existingLanguage
    importLanguageRepository.save(importLanguage)
    dataManager.populateStoredTranslations(importLanguage)
    dataManager.resetConflicts(importLanguage)
    dataManager.handleConflicts(false)
    dataManager.saveAllStoredTranslations()
  }

  fun save(import: Import): Import {
    return this.importRepository.save(import)
  }

  fun saveFile(importFile: ImportFile): ImportFile =
    importFileRepository.save(importFile)

  /**
   * Returns import when not expired.
   * When expired import is found, it is removed
   */
  fun getNotExpired(projectId: Long, authorId: Long): Import {
    return find(projectId, authorId).removeIfExpired() ?: throw NotFoundException()
  }

  fun find(projectId: Long, authorId: Long): Import? {
    return this.importRepository.findByProjectIdAndAuthorId(projectId, authorId)
  }

  private fun Import?.removeIfExpired(): Import? {
    this?.let { import ->
      if (import.createdAt == null) {
        return null
      }
      val minDate = DateUtils.addHours(currentDateProvider.getDate(), -2)
      if (minDate > import.createdAt) {
        deleteImport(import)
        throw NotFoundException(io.tolgee.constants.Message.IMPORT_HAS_EXPIRED)
      }
      return import
    }
    return null
  }

  fun findOrThrow(projectId: Long, authorId: Long) =
    this.find(projectId, authorId) ?: throw NotFoundException()

  fun findLanguages(import: Import) = importLanguageRepository.findAllByImport(import.id)

  fun findKeys(import: Import) = importKeyRepository.findAllByImport(import.id)

  fun saveLanguages(entries: Collection<ImportLanguage>) {
    importLanguageRepository.saveAll(entries)
  }

  fun findTranslations(languageId: Long) =
    this.importTranslationRepository.findAllByImportAndLanguageId(languageId)

  fun saveTranslations(translations: List<ImportTranslation>) {
    this.importTranslationRepository.saveAll(translations)
  }

  fun onExistingLanguageRemoved(language: Language) {
    this.importLanguageRepository.removeExistingLanguageReference(language)
  }

  fun saveAllImports(imports: Iterable<Import>) {
    this.importRepository.saveAll(imports)
  }

  fun saveAllFiles(files: Iterable<ImportFile>): MutableList<ImportFile>? {
    return this.importFileRepository.saveAll(files)
  }

  fun onExistingTranslationsRemoved(translationIds: Collection<Long>) {
    this.importTranslationRepository.removeExistingTranslationConflictReferences(translationIds)
  }

  fun getResult(projectId: Long, userId: Long, pageable: Pageable): Page<ImportLanguageView> {
    return this.getNotExpired(projectId, userId).let {
      this.importLanguageRepository.findImportLanguagesView(it.id, pageable)
    }
  }

  fun findLanguage(languageId: Long): ImportLanguage? {
    return importLanguageRepository.findById(languageId).orElse(null)
  }

  fun findLanguageView(languageId: Long): ImportLanguageView? {
    return importLanguageRepository.findViewById(languageId).orElse(null)
  }

  fun getTranslationsView(
    languageId: Long,
    pageable: Pageable,
    onlyConflicts: Boolean,
    onlyUnresolved: Boolean,
    search: String? = null
  ): Page<ImportTranslationView> {
    return importTranslationRepository.findImportTranslationsView(
      languageId, pageable, onlyConflicts, onlyUnresolved, search
    )
  }

  fun deleteImport(import: Import) {
    this.importTranslationRepository.deleteAllByImport(import)
    this.importLanguageRepository.deleteAllByImport(import)
    val keyIds = this.importKeyRepository.getAllIdsByImport(import)
    this.keyMetaService.deleteAllByImportKeyIdIn(keyIds)
    this.importKeyRepository.deleteByIdIn(keyIds)
    this.importFileIssueParamRepository.deleteAllByImport(import)
    this.importFileIssueRepository.deleteAllByImport(import)
    this.importFileRepository.deleteAllByImport(import)
    this.importRepository.delete(import)
  }

  @Transactional
  fun deleteImport(projectId: Long, authorId: Long) =
    this.deleteImport(findOrThrow(projectId, authorId))

  @Transactional
  fun deleteLanguage(language: ImportLanguage) {
    val import = language.file.import
    this.importTranslationRepository.deleteAllByLanguage(language)
    this.importLanguageRepository.delete(language)
    if (this.findLanguages(import = language.file.import).isEmpty()) {
      deleteImport(import)
    }
  }

  fun findTranslation(translationId: Long): ImportTranslation? {
    return importTranslationRepository.findById(translationId).orElse(null)
  }

  fun resolveTranslationConflict(translation: ImportTranslation, override: Boolean) {
    translation.override = override
    translation.resolve()
    importTranslationRepository.save(translation)
  }

  fun resolveAllOfLanguage(language: ImportLanguage, override: Boolean) {
    val translations = findTranslations(language.id)
    translations.forEach {
      it.resolve()
      it.override = override
    }
    this.importTranslationRepository.saveAll(translations)
  }

  fun findFile(fileId: Long): ImportFile? {
    return importFileRepository.findById(fileId).orElse(null)
  }

  fun getFileIssues(fileId: Long, pageable: Pageable): Page<ImportFileIssueView> {
    return importFileIssueRepository.findAllByFileIdView(fileId, pageable)
  }

  fun saveAllKeys(keys: Iterable<ImportKey>): MutableList<ImportKey> = this.importKeyRepository.saveAll(keys)

  fun saveKey(entity: ImportKey): ImportKey = this.importKeyRepository.save(entity)

  fun saveAllFileIssues(issues: Iterable<ImportFileIssue>) {
    this.importFileIssueRepository.saveAll(issues)
  }

  fun getAllByProject(projectId: Long) =
    this.importRepository.findAllByProjectId(projectId)

  fun saveAllFileIssueParams(params: List<ImportFileIssueParam>): MutableList<ImportFileIssueParam> =
    importFileIssueParamRepository.saveAll(params)
}
