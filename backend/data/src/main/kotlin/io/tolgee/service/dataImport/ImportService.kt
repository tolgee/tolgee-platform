package io.tolgee.service.dataImport

import io.sentry.Sentry
import io.tolgee.api.IImportSettings
import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.events.OnImportSoftDeleted
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
import io.tolgee.service.dataImport.status.ImportApplicationStatus
import io.tolgee.util.getSafeNamespace
import jakarta.persistence.EntityManager
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionInterceptor
import java.io.Serializable

@Service
@Transactional
class ImportService(
  private val importRepository: ImportRepository,
  private val importFileRepository: ImportFileRepository,
  private val importFileIssueRepository: ImportFileIssueRepository,
  private val importLanguageRepository: ImportLanguageRepository,
  private val importKeyRepository: ImportKeyRepository,
  private val applicationContext: ApplicationContext,
  private val importTranslationRepository: ImportTranslationRepository,
  private val importFileIssueParamRepository: ImportFileIssueParamRepository,
  private val removeExpiredImportService: RemoveExpiredImportService,
  private val entityManager: EntityManager,
  private val businessEventPublisher: BusinessEventPublisher,
  private val importDeleteService: ImportDeleteService,
  private val currentDateProvider: CurrentDateProvider,
  @Suppress("SelfReferenceConstructorParameter") @Lazy
  private val self: ImportService,
  private val fileStorage: FileStorage,
  private val tolgeeProperties: TolgeeProperties,
  private val jdbcTemplate: JdbcTemplate,
  @Lazy
  private val importSettingsService: ImportSettingsService,
) {
  @Transactional
  fun addFiles(
    files: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount,
    params: ImportAddFilesParams = ImportAddFilesParams(),
  ): MutableList<ErrorResponseBody> {
    val import =
      findNotExpired(project.id, userAccount.id) ?: Import(project).also {
        it.author = userAccount
      }

    val languages = findLanguages(import)

    if (languages.count() + files.size > 100) {
      throw BadRequestException(Message.CANNOT_ADD_MORE_THEN_100_LANGUAGES)
    }

    importRepository.save(import)
    Sentry.addBreadcrumb("Import ID: ${import.id}")

    self.saveFilesToFileStorage(import.id.toString(), files, params.storeFilesToFileStorage)

    val fileProcessor =
      CoreImportFilesProcessor(
        applicationContext = applicationContext,
        import = import,
        params = params,
        projectIcuPlaceholdersEnabled = project.icuPlaceholders,
        importSettings = importSettingsService.get(userAccount, project.id),
      )
    val errors = fileProcessor.processFiles(files)

    if (findLanguages(import).isEmpty()) {
      TransactionInterceptor.currentTransactionStatus().setRollbackOnly()
    }
    return errors
  }

  @Transactional
  fun singleStepImport(
    files: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount,
    params: SingleStepImportRequest,
    reportStatus: (ImportApplicationStatus) -> Unit,
  ) {
    reportStatus(ImportApplicationStatus.ANALYZING_FILES)
    val import = Import(project).also { it.author = userAccount }

    publishImportBusinessEvent(project.id, userAccount.id)

    val importId = "single-step-p${project.id}-a${userAccount.id}-${currentDateProvider.date.time}"
    Sentry.addBreadcrumb("Import ID: $importId")
    self.saveFilesToFileStorage(importId, files, params.storeFilesToFileStorage)

    val fileProcessor =
      CoreImportFilesProcessor(
        applicationContext = applicationContext,
        // for single step import the import entity is passed only as a holder for the import data like
        // author, project, file issues, etc
        import = import,
        params = params,
        projectIcuPlaceholdersEnabled = project.icuPlaceholders,
        importSettings = params,
        saveData = false,
      )
    val errors = fileProcessor.processFiles(files)

    if (errors.isNotEmpty()) {
      @Suppress("UNCHECKED_CAST")
      throw BadRequestException(Message.IMPORT_FAILED, errors as List<Serializable>)
    }

    if (fileProcessor.importDataManager.storedLanguages.isEmpty()) {
      throw BadRequestException(Message.NO_DATA_TO_IMPORT)
    }

    entityManager.clear()

    StoredDataImporter(
      applicationContext,
      import,
      params.forceMode,
      reportStatus,
      importSettings = params,
      _importDataManager = fileProcessor.importDataManager,
      isSingleStepImport = true,
    ).doImport()
  }

  @Transactional(noRollbackFor = [ImportConflictNotResolvedException::class])
  fun import(
    projectId: Long,
    authorId: Long,
    forceMode: ForceMode = ForceMode.NO_FORCE,
    reportStatus: (ImportApplicationStatus) -> Unit = {},
  ) {
    import(getNotExpired(projectId, authorId), forceMode, reportStatus)
  }

  @Transactional(noRollbackFor = [ImportConflictNotResolvedException::class])
  fun import(
    import: Import,
    forceMode: ForceMode = ForceMode.NO_FORCE,
    reportStatus: (ImportApplicationStatus) -> Unit = {},
  ) {
    Sentry.addBreadcrumb("Import ID: ${import.id}")
    val providedSettingsOrFromDb = importSettingsService.get(import.author, import.project.id)
    StoredDataImporter(applicationContext, import, forceMode, reportStatus, providedSettingsOrFromDb).doImport()
    deleteImport(import)
    publishImportBusinessEvent(import.project.id, import.author.id)
  }

  private fun publishImportBusinessEvent(
    projectId: Long,
    authorId: Long,
  ) {
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "IMPORT",
        projectId = projectId,
        userAccountId = authorId,
      ),
    )
  }

  @Transactional
  fun selectExistingLanguage(
    importLanguage: ImportLanguage,
    existingLanguage: Language?,
  ) {
    if (importLanguage.existingLanguage == existingLanguage) {
      return
    }
    val import = importLanguage.file.import
    Sentry.addBreadcrumb("Import ID: ${import.id}")
    val dataManager = ImportDataManager(applicationContext, import)
    val oldExistingLanguage = importLanguage.existingLanguage
    importLanguage.existingLanguage = existingLanguage
    importLanguageRepository.save(importLanguage)
    dataManager.resetLanguage(importLanguage)
    dataManager.resetCollisionsBetweenFiles(importLanguage, oldExistingLanguage)
  }

  @Transactional
  fun selectNamespace(
    projectId: Long,
    authorId: Long,
    fileId: Long,
    namespace: String?,
  ) {
    val file = findFile(projectId, authorId, fileId) ?: throw NotFoundException()
    val import = file.import
    Sentry.addBreadcrumb("Import ID: ${import.id}")
    val dataManager = ImportDataManager(applicationContext, import)
    file.namespace = getSafeNamespace(namespace)
    importFileRepository.save(file)
    file.languages.forEach {
      dataManager.resetLanguage(it)
      dataManager.resetCollisionsBetweenFiles(it, null)
    }
  }

  fun save(import: Import): Import {
    return this.importRepository.save(import)
  }

  fun saveFile(importFile: ImportFile): ImportFile = importFileRepository.save(importFile)

  /**
   * Returns import when not expired.
   * When expired import is found, it is removed
   */
  fun getNotExpired(
    projectId: Long,
    authorId: Long,
  ): Import {
    return findNotExpired(projectId, authorId) ?: throw NotFoundException()
  }

  fun findDeleted(importId: Long): Import? {
    return importRepository.findDeleted(importId)
  }

  private fun findNotExpired(
    projectId: Long,
    userAccountId: Long,
  ): Import? {
    val import = this.find(projectId, userAccountId)
    return removeExpiredImportService.removeIfExpired(import)
  }

  fun find(
    projectId: Long,
    authorId: Long,
  ): Import? {
    return this.importRepository.findByProjectIdAndAuthorId(projectId, authorId)
  }

  fun get(
    projectId: Long,
    authorId: Long,
  ): Import {
    return this.find(projectId, authorId) ?: throw NotFoundException()
  }

  fun get(id: Long): Import {
    return importRepository.findById(id).orElse(null) ?: throw NotFoundException()
  }

  fun findLanguages(import: Import) = importLanguageRepository.findAllByImport(import.id)

  @Suppress("UNCHECKED_CAST")
  fun findKeys(import: Import): List<ImportKey> {
    var result: List<ImportKey> =
      entityManager.createQuery(
        """
            select distinct ik from ImportKey ik 
            left join fetch ik.keyMeta ikm
            left join fetch ikm.comments ikc
            join ik.file if
            where if.import = :import
            """,
      )
        .setParameter("import", import)
        .resultList as List<ImportKey>

    // when we are import very lot of keys, we need to split it to multiple queries due to parameter limit
    result =
      result.chunked(30000).flatMap { subresult ->
        entityManager.createQuery(
          """
            select distinct ik from ImportKey ik 
            left join fetch ik.keyMeta ikm
            left join fetch ikm.codeReferences ikc
            join ik.file if
            where ik in :keys
        """,
        ).setParameter("keys", subresult)
          .resultList as List<ImportKey>
      }

    return result
  }

  fun saveLanguages(entries: Collection<ImportLanguage>) {
    importLanguageRepository.saveAll(entries)
  }

  fun findTranslations(languageId: Long) = this.importTranslationRepository.findAllByImportAndLanguageId(languageId)

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

  fun getResult(
    projectId: Long,
    userId: Long,
    pageable: Pageable,
  ): Page<ImportLanguageView> {
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
    search: String? = null,
  ): Page<ImportTranslationView> {
    return importTranslationRepository.findImportTranslationsView(
      languageId,
      pageable,
      onlyConflicts,
      onlyUnresolved,
      search,
    )
  }

  @Transactional
  fun deleteImport(import: Import) {
    import.deletedAt = currentDateProvider.date
    importRepository.save(import)
    applicationContext.publishEvent(OnImportSoftDeleted(import.id))
  }

  @Transactional
  fun hardDeleteImport(import: Import) {
    importDeleteService.deleteImport(import.id)
  }

  @Transactional
  fun deleteImport(
    projectId: Long,
    authorId: Long,
  ) = this.deleteImport(get(projectId, authorId))

  @Transactional
  fun deleteLanguage(language: ImportLanguage) {
    val import = language.file.import
    this.importTranslationRepository.deleteAllByLanguage(language)
    this.importLanguageRepository.delete(language)
    if (this.findLanguages(import = language.file.import).isEmpty()) {
      deleteImport(import)
      return
    }
    entityManager.clear()
    entityManager.refresh(entityManager.merge(import))
    val dataManager = ImportDataManager(applicationContext, import)
    dataManager.resetCollisionsBetweenFiles(language, null)
  }

  fun findTranslation(translationId: Long): ImportTranslation? {
    return importTranslationRepository.findById(translationId).orElse(null)
  }

  fun findTranslation(
    translationId: Long,
    languageId: Long,
  ): ImportTranslation? {
    return importTranslationRepository.findByIdAndLanguageId(translationId, languageId)
  }

  fun resolveTranslationConflict(
    translationId: Long,
    languageId: Long,
    override: Boolean,
  ) {
    val translation = findTranslation(translationId, languageId) ?: throw NotFoundException()
    translation.override = override
    translation.resolve()
    importTranslationRepository.save(translation)
  }

  fun resolveAllOfLanguage(
    language: ImportLanguage,
    override: Boolean,
  ) {
    val translations = findTranslations(language.id)
    translations.forEach {
      it.resolve()
      it.override = override
    }
    this.importTranslationRepository.saveAll(translations)
  }

  fun findFile(
    projectId: Long,
    authorId: Long,
    fileId: Long,
  ): ImportFile? {
    return importFileRepository.finByProjectAuthorAndId(projectId, authorId, fileId)
  }

  fun getFileIssues(
    projectId: Long,
    authorId: Long,
    fileId: Long,
    pageable: Pageable,
  ): Page<ImportFileIssueView> {
    val file = findFile(projectId, authorId, fileId) ?: throw NotFoundException()
    return importFileIssueRepository.findAllByFileIdView(file.id, pageable)
  }

  fun saveAllKeys(keys: Iterable<ImportKey>): MutableList<ImportKey> = this.importKeyRepository.saveAll(keys)

  fun saveKey(entity: ImportKey): ImportKey = this.importKeyRepository.save(entity)

  fun saveAllFileIssues(issues: Iterable<ImportFileIssue>) {
    this.importFileIssueRepository.saveAll(issues)
    this.saveAllFileIssueParams(issues.flatMap { it.params })
  }

  fun getAllByProject(projectId: Long) = this.importRepository.findAllByProjectId(projectId)

  fun saveAllFileIssueParams(params: List<ImportFileIssueParam>): MutableList<ImportFileIssueParam> =
    importFileIssueParamRepository.saveAll(params)

  fun getAllNamespaces(importId: Long) = importRepository.getAllNamespaces(importId)

  /**
   * This function saves the files to file storage.
   * When import fails, we need the files for future debugging
   */
  @Async
  fun saveFilesToFileStorage(
    importId: String,
    files: List<ImportFileDto>,
    storeFilesToFileStorage: Boolean,
  ) {
    if (tolgeeProperties.import.storeFilesForDebugging && storeFilesToFileStorage) {
      files.forEach {
        fileStorage.storeFile(getFileStoragePath(importId, it.name), it.data)
      }
    }
  }

  fun getFileStoragePath(
    importId: String,
    fileName: String,
  ): String {
    val notBlankFilename = fileName.ifBlank { "blank_name" }
    return "${getFileStorageImportRoot(importId)}/$notBlankFilename"
  }

  private fun getFileStorageImportRoot(importId: String) = "importFiles/$importId"

  fun deleteAllBetweenFileCollisionsForFiles(
    ids: List<Long>,
    importLanguageIds: List<Long>,
  ) {
    val languageIdStrings = importLanguageIds.map { it.toString() }
    entityManager.createNativeQuery(
      """
      with deleted as (  
        delete from import_file_issue_param
        where issue_id in (
          select import_file_issue.id from import_file_issue
          join import_file_issue_param ifip on 
              import_file_issue.id = ifip.issue_id 
              and ifip.type = 2 
              and ifip.value in :importLanguageIds
          where file_id in :ids and import_file_issue.type = 10
        ) returning issue_id
      )
      delete from import_file_issue
      where id in (select issue_id from deleted)
    """,
    )
      .setParameter("ids", ids)
      .setParameter("importLanguageIds", languageIdStrings)
      .executeUpdate()
  }

  fun updateIsSelectedForTranslations(translations: List<ImportTranslation>) {
    jdbcTemplate.batchUpdate(
      "update import_translation set is_selected_to_import = ? where id = ?",
      translations,
      100,
    ) { ps, entity ->
      ps.setBoolean(1, entity.isSelectedToImport)
      ps.setLong(2, entity.id)
    }
    entityManager.clear()
  }

  fun applySettings(
    userAccount: UserAccount,
    projectId: Long,
    oldSettings: IImportSettings,
    newSettings: IImportSettings,
  ) {
    find(projectId, userAccount.id)?.let {
      applySettings(it, oldSettings, newSettings)
      save(it)
    }
  }

  fun applySettings(
    import: Import,
    oldSettings: IImportSettings,
    newSettings: IImportSettings,
  ) {
    ImportDataManager(applicationContext, import).applySettings(oldSettings, newSettings)
  }

  fun findTranslationsForPlaceholderConversion(importId: Long): List<ImportTranslation> {
    return importTranslationRepository.findTranslationsForPlaceholderConversion(importId)
  }
}
