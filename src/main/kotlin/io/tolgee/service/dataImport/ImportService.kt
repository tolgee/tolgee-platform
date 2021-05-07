package io.tolgee.service.dataImport

import io.tolgee.constants.Message
import io.tolgee.dtos.ImportDto
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.ImportConflictNotResolvedException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.Key
import io.tolgee.model.Language
import io.tolgee.model.Repository
import io.tolgee.model.Translation
import io.tolgee.model.dataImport.*
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.views.ImportFileIssueView
import io.tolgee.model.views.ImportLanguageView
import io.tolgee.model.views.ImportTranslationView
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.dataImport.*
import io.tolgee.repository.dataImport.issues.ImportFileIssueRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.OutputStream
import java.util.stream.Collectors

@Service
@Transactional
class ImportService(
        private val languageService: LanguageService,
        private val keyService: KeyService,
        private val keyRepository: KeyRepository,
        private val translationRepository: TranslationRepository,
        private val importRepository: ImportRepository,
        private val importFileRepository: ImportFileRepository,
        private val authenticationFacade: AuthenticationFacade,
        private val repositoryHolder: RepositoryHolder,
        private val importFileIssueRepository: ImportFileIssueRepository,
        private val importArchiveRepository: ImportArchiveRepository,
        private val importLanguageRepository: ImportLanguageRepository,
        private val importKeyRepository: ImportKeyRepository,
        private val applicationContext: ApplicationContext,
        private val importTranslationRepository: ImportTranslationRepository
) {
    @Autowired
    private lateinit var translationService: TranslationService

    @Transactional
    @Deprecated("Use doImport")
    fun import(repository: Repository, dto: ImportDto, emitter: OutputStream) {
        val language = languageService.getOrCreate(repository, dto.languageAbbreviation!!)
        val allKeys = keyService.getAll(repository.id).stream().collect(Collectors.toMap({ it.name }, { it }))
        val allTranslations = translationService.getAllByLanguageId(language.id!!)
                .stream()
                .collect(Collectors.toMap({ it.key!!.id }, { it }))

        val keysToSave = ArrayList<Key>()
        val translationsToSave = ArrayList<Translation>()

        for ((index, entry) in dto.data!!.entries.withIndex()) {
            val key = allKeys[entry.key] ?: run {
                val keyToSave = Key(name = entry.key, repository = repository)
                keysToSave.add(keyToSave)
                keyToSave
            }

            val translation = allTranslations[key.id] ?: Translation()
            translation.key = key
            translation.language = language
            translation.text = entry.value
            translationsToSave.add(translation)
            emitter.write(index)
        }

        keyRepository.saveAll(keysToSave)
        translationRepository.saveAll(translationsToSave)
    }

    @Transactional
    fun addFiles(files: List<ImportFileDto>,
                 messageClient: ((ImportStreamingProgressMessageType, List<Any>?) -> Unit)? = null) {
        val import = find(repositoryHolder.repository.id, authenticationFacade.userAccount.id!!)
                ?: Import(authenticationFacade.userAccount, repositoryHolder.repository)

        val nonNullMessageClient = messageClient ?: { _, _ -> }

        importRepository.save(import)
        val fileProcessor = CoreImportFilesProcessor(
                applicationContext = applicationContext,
                import = import
        )
        fileProcessor.processFiles(files, nonNullMessageClient)
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

    fun saveArchive(importArchive: ImportArchive): ImportArchive = importArchiveRepository.save(importArchive)

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

    fun deleteImport(import: Import) =
            this.importRepository.delete(import)

    fun deleteImport(repositoryId: Long, authorId: Long) =
            this.deleteImport(findOrThrow(repositoryId, authorId))

    fun deleteLanguage(language: ImportLanguage) {
        val import = language.file.import
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
}
