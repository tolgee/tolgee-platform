package io.tolgee.development.testDataBuilder

import io.tolgee.service.*
import io.tolgee.service.dataImport.ImportService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class TestDataService(
        private val userAccountService: UserAccountService,
        private val repositoryService: RepositoryService,
        private val languageService: LanguageService,
        private val importService: ImportService,
        private val keyService: KeyService,
        private val keyMetaService: KeyMetaService,
        private val translationService: TranslationService,
        private val permissionService: PermissionService,
        private val entityManager: EntityManager
) {
    @Transactional
    fun saveTestData(builder: TestDataBuilder) {
        userAccountService.saveAll(builder.data.userAccounts.map {
            it.self.password = userAccountService.encodePassword(it.rawPassword)
            it.self
        })
        repositoryService.saveAll(builder.data.repositories.map { it.self })
        permissionService.saveAll(builder.data.repositories.flatMap { it.data.permissions.map { it.self } })
        val languages = builder.data.repositories.flatMap { it.data.languages.map { it.self } }
        languageService.saveAll(languages)

        builder.data.repositories.flatMap { it.data.keys.map { it.self } }.let {
            keyService.saveAll(it)
        }

        builder.data.repositories.flatMap { it.data.translations.map { it.self } }.let {
            translationService.saveAll(it)
        }

        val importBuilders = builder.data.repositories.flatMap { repoBuilder -> repoBuilder.data.imports }
        importService.saveAllImports(importBuilders.map { it.self })
        val importFileBuilders = importBuilders.flatMap { it.data.importFiles }
        importService.saveAllFiles(importFileBuilders.map { it.self })
        importService.saveTranslations(importFileBuilders.flatMap { it.data.importTranslations.map { it.self } })
        importService.saveLanguages(importFileBuilders.flatMap { it.data.importLanguages.map { it.self } })
        entityManager.flush()
        entityManager.clear()
    }

    fun saveTestData(ft: TestDataBuilder.() -> Unit): TestDataBuilder {
        val builder = TestDataBuilder()
        ft(builder)
        saveTestData(builder)
        return builder
    }
}
