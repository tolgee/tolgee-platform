package io.tolgee.service.dataImport

import io.tolgee.constants.Message
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ErrorResponseBody
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Language
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.*
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.repository.dataImport.*
import io.tolgee.repository.dataImport.issues.ImportFileIssueParamRepository
import io.tolgee.repository.dataImport.issues.ImportFileIssueRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyMetaService
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
        private val authenticationFacade: AuthenticationFacade,
        private val repositoryHolder: RepositoryHolder,
        private val importFileIssueRepository: ImportFileIssueRepository,
        private val importLanguageRepository: ImportLanguageRepository,
        private val importKeyRepository: ImportKeyRepository,
        private val applicationContext: ApplicationContext,
        private val importTranslationRepository: ImportTranslationRepository,
        private val importFileIssueParamRepository: ImportFileIssueParamRepository,
        private val keyMetaService: KeyMetaService,
        private val entityManager: EntityManager
) {
    @Transactional
    fun addFiles(files: List<ImportFileDto>,
                 messageClient: ((ImportStreamingProgressMessageType, List<Any>?) -> Unit)? = null
    ): List<ErrorResponseBody> {
        val import = find(repositoryHolder.repository.id, authenticationFacade.userAccount.id!!)
                ?: Import(authenticationFacade.userAccount, repositoryHolder.repository)

        val nonNullMessageClient = messageClient ?: { _, _ -> }

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
    fun import(repositoryId: Long, authorId: Long, forceMode: ForceMode = ForceMode.NO_FORCE) {
        import(findOrThrow(repositoryId, authorId), forceMode)
    }


    @Transactional(noRollbackFor = [ImportConflictNotResolvedException::class])
    fun import(import: Import, forceMode: ForceMode = ForceMode.NO_FORCE) {
        StoredDataImporter(applicationContext, import, forceMode).doImport()
        deleteImport(import)
    }

    @Transactional
    fun selectExistingLanguage(importLanguage: ImportLanguage, existingLanguage: Language) {
        val import = importLanguage.file.import
        val dataManager = ImportDataManager(applicationContext, import)
        if (dataManager.storedLanguages.any { it.existingLanguage?.id == existingLanguage.id }) {
            throw BadRequestException(Message.LANGUAGE_ALREADY_SELECTED)
        }
        importLanguage.existingLanguage = existingLanguage
        importLanguageRepository.save(importLanguage)
        dataManager.populateStoredTranslations(importLanguage)
        dataManager.handleConflicts()
        dataManager.saveAllStoredTranslations()
    }

    fun save(import: Import): Import {
        return this.importRepository.save(import)
    }

    fun saveFile(importFile: ImportFile): ImportFile =
            importFileRepository.save(importFile)

    fun saveFileIssue(importFileIssue: ImportFileIssue): ImportFileIssue =
            importFileIssueRepository.save(importFileIssue)

    fun find(repositoryId: Long, authorId: Long) =
            this.importRepository.findByRepositoryIdAndAuthorId(repositoryId, authorId)

    fun findOrThrow(repositoryId: Long, authorId: Long) =
            this.find(repositoryId, authorId) ?: throw NotFoundException()

    fun findLanguages(import: Import) = importLanguageRepository.findAllByImport(import.id)

    fun findKeys(import: Import) = importKeyRepository.findAllByImport(import.id)

    fun saveLanguages(entries: Collection<ImportLanguage>) {
        importLanguageRepository.saveAll(entries)
    }

    fun findTranslations(import: Import, languageId: Long) =
            this.importTranslationRepository.findAllByImportAndLanguageId(import, languageId)

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

    fun onTranslationConflictRemoved(translation: Translation) {
        this.importTranslationRepository.removeExistingTranslationConflictReference(translation)
    }

    fun getResult(repositoryId: Long, userId: Long, pageable: Pageable): Page<ImportLanguageView> {
        return this.find(repositoryId, userId)?.let {
            this.importLanguageRepository.findImportLanguagesView(it.id, pageable)
        } ?: throw NotFoundException()
    }

    fun findLanguage(languageId: Long): ImportLanguage? {
        return importLanguageRepository.findById(languageId).orElse(null)
    }

    fun getTranslations(languageId: Long, pageable: Pageable, onlyConflicts: Boolean, onlyUnresolved: Boolean):
            Page<ImportTranslationView> {
        return importTranslationRepository.findImportTranslationsView(languageId, pageable, onlyConflicts, onlyUnresolved)
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
    fun deleteImport(repositoryId: Long, authorId: Long) =
            this.deleteImport(findOrThrow(repositoryId, authorId))

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
        translation.resolved = true
        importTranslationRepository.save(translation)
    }

    fun resolveAllOfLanguage(language: ImportLanguage, override: Boolean) {
        return this.importTranslationRepository.resolveAllOfLanguage(language, override)
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

    fun getAllByRepository(repositoryId: Long) =
            this.importRepository.findAllByRepositoryId(repositoryId)

    fun saveAllFileIssueParams(params: List<ImportFileIssueParam>): MutableList<ImportFileIssueParam> =
            importFileIssueParamRepository.saveAll(params)
}
