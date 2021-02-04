package io.tolgee.development

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.ApiScope
import io.tolgee.dtos.request.LanguageDTO
import io.tolgee.dtos.request.SignUpDto
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.*
import io.tolgee.repository.ApiKeyRepository
import io.tolgee.repository.RepositoryRepository
import io.tolgee.repository.UserAccountRepository
import io.tolgee.security.InitialPasswordManager
import io.tolgee.service.LanguageService
import io.tolgee.service.PermissionService
import io.tolgee.service.UserAccountService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
open class DbPopulatorReal(private val entityManager: EntityManager,
                           private val userAccountRepository: UserAccountRepository,
                           private val permissionService: PermissionService,
                           private val userAccountService: UserAccountService,
                           private val languageService: LanguageService,
                           private val repositoryRepository: RepositoryRepository,
                           private val apiKeyRepository: ApiKeyRepository,
                           private val tolgeeProperties: TolgeeProperties,
                           private val initialPasswordManager: InitialPasswordManager
) {
    private var de: Language? = null
    private var en: Language? = null

    @Transactional
    open fun autoPopulate() {
        //do not populate if db is not empty
        if (userAccountRepository.count() == 0L) {
            this.populate("Application")
        }
    }

    open fun createUser(username: String, password: String? = null): UserAccount {
        return userAccountService.getByUserName(username).orElseGet {
            val signUpDto = SignUpDto(name = username, email = username, password = password
                    ?: initialPasswordManager.initialPassword)
            userAccountService.createUser(signUpDto)
        }
    }

    open fun createUser(username: String): UserAccount {
        return createUser(username, null)
    }

    @Transactional
    open fun createBase(repositoryName: String?, username: String, password: String? = null): Repository {
        val userAccount = createUser(username, password)
        val repository = Repository()
        repository.name = repositoryName
        repository.createdBy = userAccount
        en = createLanguage("en", repository)
        de = createLanguage("de", repository)
        permissionService.grantFullAccessToRepo(userAccount, repository)
        repositoryRepository.saveAndFlush(repository)
        entityManager.flush()
        entityManager.clear()
        return repository
    }

    @Transactional
    open fun createBase(repositoryName: String?, username: String): Repository {
        return createBase(repositoryName, username, null)
    }

    @Transactional
    open fun createBase(repositoryName: String?): Repository {
        return createBase(repositoryName, tolgeeProperties.authentication.initialUsername)
    }

    @Transactional
    open fun populate(repositoryName: String?): Repository {
        return populate(repositoryName, tolgeeProperties.authentication.initialUsername)
    }

    @Transactional
    open fun populate(repositoryName: String?, userName: String): Repository {
        val repository = createBase(repositoryName, userName)
        createApiKey(repository)
        createTranslation(repository, "Hello world!", "Hallo Welt!", en, de)
        createTranslation(repository, "English text one.", "Deutsch text einz.", en, de)
        createTranslation(repository, "This is translation in home folder",
                "Das ist die Übersetzung im Home-Ordner", en, de)
        createTranslation(repository, "This is translation in news folder",
                "Das ist die Übersetzung im News-Ordner", en, de)
        createTranslation(repository, "This is another translation in news folder",
                "Das ist eine weitere Übersetzung im Nachrichtenordner", en, de)
        createTranslation(repository, "This is standard text somewhere in DOM.",
                "Das ist Standardtext irgendwo im DOM.", en, de)
        createTranslation(repository, "This is another standard text somewhere in DOM.",
                "Das ist ein weiterer Standardtext irgendwo in DOM.", en, de)
        createTranslation(repository, "This is translation retrieved by service.",
                "Dase Übersetzung wird vom Service abgerufen.", en, de)
        createTranslation(repository, "This is textarea with placeholder and value.",
                "Das ist ein Textarea mit Placeholder und Value.", en, de)
        createTranslation(repository, "This is textarea with placeholder.",
                "Das ist ein Textarea mit Placeholder.", en, de)
        createTranslation(repository, "This is input with value.",
                "Das ist ein Input mit value.", en, de)
        createTranslation(repository, "This is input with placeholder.",
                "Das ist ein Input mit Placeholder.", en, de)
        return repository
    }

    private fun createApiKey(repository: Repository) {
        val (_, user) = repository.permissions.stream().findAny().orElseThrow { NotFoundException() }
        if (apiKeyRepository.findByKey(API_KEY).isEmpty) {
            val apiKey = ApiKey.builder()
                    .repository(repository)
                    .key(API_KEY)
                    .userAccount(user)
                    .scopes(setOf(*ApiScope.values()))
                    .build()
            repository.apiKeys.add(apiKey)
            apiKeyRepository.save(apiKey)
        }
    }

    private fun createLanguage(name: String, repository: Repository): Language {
        return languageService.createLanguage(LanguageDTO(null, name, name), repository)
    }

    private fun createTranslation(repository: Repository, english: String,
                                  deutsch: String, en: Language?, de: Language?) {
        val key = Key()
        key.name = "sampleApp." + english.replace(" ", "_").toLowerCase().replace("\\.+$".toRegex(), "")
        key.repository = repository
        val translation = Translation()
        translation.language = en
        translation.key = key
        translation.text = english
        key.translations.add(translation)
        key.translations.add(translation)
        entityManager.persist(translation)
        val translationDe = Translation()
        translationDe.language = de
        translationDe.key = key
        translationDe.text = deutsch
        key.translations.add(translationDe)
        entityManager.persist(translationDe)
        entityManager.persist(key)
        entityManager.flush()
    }

    companion object {
        const val API_KEY = "this_is_dummy_api_key"
    }
}