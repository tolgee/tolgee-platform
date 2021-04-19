package io.tolgee.service.import

import io.tolgee.dtos.ImportDto
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.dtos.dataImport.ImportStreamingProgressMessageType
import io.tolgee.model.Key
import io.tolgee.model.Repository
import io.tolgee.model.Translation
import io.tolgee.model.UserAccount
import io.tolgee.model.import.Import
import io.tolgee.model.import.ImportArchive
import io.tolgee.model.import.ImportFile
import io.tolgee.model.import.issues.ImportFileIssue
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import io.tolgee.repository.import.ImportArchiveRepository
import io.tolgee.repository.import.ImportFileRepository
import io.tolgee.repository.import.ImportRepository
import io.tolgee.repository.import.issues.ImportFileIssueRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.security.repository_auth.RepositoryHolder
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.TranslationService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.OutputStream
import java.util.stream.Collectors

@Service
class ImportService(
        private val languageService: LanguageService,
        private val keyService: KeyService,
        private val keyRepository: KeyRepository,
        private val translationRepository: TranslationRepository,
        private val importRepository: ImportRepository,
        private val importFileProcessor: ImportFileProcessor,
        private val importFileRepository: ImportFileRepository,
        private val authenticationFacade: AuthenticationFacade,
        private val repositoryHolder: RepositoryHolder,
        private val importFileIssueRepository: ImportFileIssueRepository,
        private val importArchiveRepository: ImportArchiveRepository
) {
    @Autowired
    private lateinit var translationService: TranslationService

    @Transactional
    @Deprecated("Use doImport")
    fun import(repository: Repository, dto: ImportDto, emitter: OutputStream) {
        val language = languageService.getOrCreate(repository, dto.languageAbbreviation)
        val allKeys = keyService.getAll(repository.id).stream().collect(Collectors.toMap({ it.name }, { it }))
        val allTranslations = translationService.getAllByLanguageId(language.id)
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
    fun doImport(files: List<ImportFileDto>,
                 messageClient: (ImportStreamingProgressMessageType, List<Any>?) -> Unit) {
        Import(authenticationFacade.userAccount, repositoryHolder.repository)
                .let {
                    importRepository.save(it)
                    importFileProcessor.import = it
                }

        importFileProcessor.processFiles(files, messageClient)
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

    fun saveArchive(importArchive: ImportArchive): ImportArchive = importArchiveRepository.save(importArchive)


}
