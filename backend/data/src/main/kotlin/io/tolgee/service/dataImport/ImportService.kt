package io.tolgee.service.dataImport

import io.tolgee.component.reporting.BusinessEventPublisher
import io.tolgee.component.reporting.OnBusinessEventToCaptureEvent
import io.tolgee.dtos.dataImport.ImportAddFilesParams
import io.tolgee.dtos.dataImport.ImportFileDto
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
import io.tolgee.service.key.KeyMetaService
import io.tolgee.util.getSafeNamespace
import org.hibernate.annotations.QueryHints
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionInterceptor
import javax.persistence.EntityManager

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
  private val keyMetaService: KeyMetaService,
  private val removeExpiredImportService: RemoveExpiredImportService,
  private val entityManager: EntityManager,
  private val businessEventPublisher: BusinessEventPublisher
) {
  @Transactional
  fun addFiles(
    files: List<ImportFileDto>,
    project: Project,
    userAccount: UserAccount,
    params: ImportAddFilesParams = ImportAddFilesParams()
  ): List<ErrorResponseBody> {
    val import = findNotExpired(project.id, userAccount.id) ?: Import(project).also {
      it.author = userAccount
    }

    val languages = findLanguages(import)

    if (languages.count() + files.size > 100) {
      throw BadRequestException(io.tolgee.constants.Message.CANNOT_ADD_MORE_THEN_100_LANGUAGES)
    }

    importRepository.save(import)
    val fileProcessor = CoreImportFilesProcessor(
      applicationContext = applicationContext,
      import = import,
      params = params
    )
    val errors = fileProcessor.processFiles(files)

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
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "IMPORT",
        projectId = import.project.id,
        userAccountId = import.author.id
      )
    )
  }

  @Transactional
  fun selectExistingLanguage(importLanguage: ImportLanguage, existingLanguage: Language?) {
    if (importLanguage.existingLanguage == existingLanguage) {
      return
    }
    val import = importLanguage.file.import
    val dataManager = ImportDataManager(applicationContext, import)
    existingLanguage?.let {
      val langAlreadySelectedInTheSameNS = dataManager.storedLanguages.any {
        it.existingLanguage?.id == existingLanguage.id && it.file.namespace == importLanguage.file.namespace
      }
      if (langAlreadySelectedInTheSameNS) {
        throw BadRequestException(io.tolgee.constants.Message.LANGUAGE_ALREADY_SELECTED)
      }
    }
    importLanguage.existingLanguage = existingLanguage
    importLanguageRepository.save(importLanguage)
    dataManager.resetLanguage(importLanguage)
  }

  @Transactional
  fun selectNamespace(file: ImportFile, namespace: String?) {
    val import = file.import
    val dataManager = ImportDataManager(applicationContext, import)
    file.namespace = getSafeNamespace(namespace)
    importFileRepository.save(file)
    file.languages.forEach {
      dataManager.resetLanguage(it)
    }
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
    return findNotExpired(projectId, authorId) ?: throw NotFoundException()
  }

  private fun findNotExpired(projectId: Long, userAccountId: Long): Import? {
    val import = this.find(projectId, userAccountId)
    return removeExpiredImportService.removeIfExpired(import)
  }

  fun find(projectId: Long, authorId: Long): Import? {
    return this.importRepository.findByProjectIdAndAuthorId(projectId, authorId)
  }

  fun get(projectId: Long, authorId: Long): Import {
    return this.find(projectId, authorId) ?: throw NotFoundException()
  }

  fun get(id: Long): Import {
    return importRepository.findById(id).orElse(null) ?: throw NotFoundException()
  }

  fun findLanguages(import: Import) = importLanguageRepository.findAllByImport(import.id)

  @Suppress("UNCHECKED_CAST")

  fun findKeys(import: Import): List<ImportKey> {
    var result: List<ImportKey> = entityManager.createQuery(
      """
            select distinct ik from ImportKey ik 
            left join fetch ik.keyMeta ikm
            left join fetch ikm.comments ikc
            join ik.file if
            where if.import = :import
            """
    )
      .setParameter("import", import)
      .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
      .resultList as List<ImportKey>

    result = entityManager.createQuery(
      """
            select distinct ik from ImportKey ik 
            left join fetch ik.keyMeta ikm
            left join fetch ikm.codeReferences ikc
            join ik.file if
            where ik in :keys
        """
    ).setParameter("keys", result)
      .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
      .resultList as List<ImportKey>

    return result
  }

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
    this.deleteImport(get(projectId, authorId))

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

  fun getAllNamespaces(importId: Long) = importRepository.getAllNamespaces(importId)
}
