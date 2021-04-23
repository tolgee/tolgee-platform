package io.tolgee.development.testDataBuilder

import io.tolgee.service.LanguageService
import io.tolgee.service.RepositoryService
import io.tolgee.service.UserAccountService
import io.tolgee.service.dataImport.ImportService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TestDataService(
        private val userAccountService: UserAccountService,
        private val repositoryService: RepositoryService,
        private val languageService: LanguageService,
        private val importService: ImportService
) {
    @Transactional
    fun saveTestData(builder: TestDataBuilder) {
        userAccountService.saveAll(builder.data.userAccounts)
        repositoryService.saveAll(builder.data.repositories.map { it.self })
        val languages = builder.data.repositories.flatMap { it.data.languages }
        languageService.saveAll(languages)
        val importBuilders = builder.data.repositories.flatMap { repoBuilder -> repoBuilder.data.imports }
        importService.saveAllImports(importBuilders.map { it.self })
        val importFileBuilders = importBuilders.flatMap { it.data.importFiles }
        importService.saveAllFiles(importFileBuilders.map { it.self })
        importService.saveTranslations(importFileBuilders.flatMap { it.data.importTranslations })
        importService.saveLanguages(importFileBuilders.flatMap { it.data.importLanguages })
    }

    fun buildTestData(ft: TestDataBuilder.() -> Unit): TestDataBuilder {
        val builder = TestDataBuilder()
        ft(builder)
        saveTestData(builder)
        return builder
    }
}
