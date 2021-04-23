package io.tolgee.development.testDataBuilder

import io.tolgee.model.Language
import io.tolgee.model.Organization
import io.tolgee.model.Repository
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.*

class DataBuilders {
    class RepositoryBuilder(userOwner: UserAccount? = null,
                            organizationOwner: Organization? = null,
                            testDataBuilder: TestDataBuilder
    ) : EntityDataBuilder<Repository> {
        override var self: Repository = Repository().apply {
            if (userOwner == null && organizationOwner == null) {
                if (testDataBuilder.data.userAccounts.size > 0) {
                    this.userOwner = testDataBuilder.data.userAccounts.first()
                } else if (testDataBuilder.data.organizations.size > 0) {
                    this.organizationOwner = testDataBuilder.data.organizations.first().self
                }
                return@apply
            }

            this.userOwner = userOwner
            this.organizationOwner = organizationOwner
        }

        class DATA {
            val languages = mutableListOf<Language>()
            val imports = mutableListOf<ImportBuilder>()
        }

        var data = DATA()

        fun addImport(author: UserAccount? = null, ft: ImportBuilder.() -> Unit): ImportBuilder {
            return ImportBuilder(author, this).apply {
                this@RepositoryBuilder.data.imports.add(this)
                ft(this)
            }
        }

        fun addLanguage(ft: Language.() -> Unit): Language {
            val language = Language()
            data.languages.add(language)
            ft(language)
            return language
        }
    }

    class ImportBuilder(author: UserAccount? = null, repositoryBuilder: RepositoryBuilder) : EntityDataBuilder<Import> {
        class DATA {
            val importFiles = mutableListOf<ImportFileBuilder>()
        }

        val data = DATA()

        override var self: Import = Import(author ?: repositoryBuilder.self.userOwner!!, repositoryBuilder.self)

        fun addFile(ft: ImportFileBuilder.() -> Unit): ImportFileBuilder {
            val importFileBuilder = ImportFileBuilder(this)
            data.importFiles.add(importFileBuilder)
            ft(importFileBuilder)
            return importFileBuilder
        }

    }

    class ImportFileBuilder(importBuilder: ImportBuilder) : EntityDataBuilder<ImportFile> {
        override var self: ImportFile = ImportFile("lang.json", importBuilder.self)

        class DATA {
            val importKeys = mutableListOf<ImportKeyBuilder>()
            val importLanguages = mutableListOf<ImportLanguage>()
            val importTranslations = mutableListOf<ImportTranslation>()
        }

        val data = DATA()


        fun addKey(ft: ImportKeyBuilder.() -> Unit): ImportKeyBuilder {
            val importKeyBuilder = ImportKeyBuilder(this)
            data.importKeys.add(importKeyBuilder)
            ft(importKeyBuilder)
            return importKeyBuilder
        }

        fun addLanguage(ft: ImportLanguageBuilder.() -> Unit): ImportLanguageBuilder {
            val importLanguageBuilder = ImportLanguageBuilder(this)
            data.importLanguages += importLanguageBuilder.self
            ft(importLanguageBuilder)
            return importLanguageBuilder
        }

        fun addTranslation(ft: ImportTranslationBuilder.() -> Unit): ImportTranslationBuilder {
            val importTranslationBuilder = ImportTranslationBuilder(this)
            data.importTranslations += importTranslationBuilder.self
            ft(importTranslationBuilder)
            return importTranslationBuilder
        }
    }

    class ImportKeyBuilder(
            importFileBuilder: ImportFileBuilder
    ) : EntityDataBuilder<ImportKey> {
        override var self: ImportKey = ImportKey("testKey").apply {
            files.add(importFileBuilder.self)
        }

    }

    class ImportLanguageBuilder(
            importFileBuilder: ImportFileBuilder
    ) : EntityDataBuilder<ImportLanguage> {
        override var self: ImportLanguage = ImportLanguage("en", importFileBuilder.self)

    }

    class ImportTranslationBuilder(
            importFileBuilder: ImportFileBuilder
    ) : EntityDataBuilder<ImportTranslation> {
        override var self: ImportTranslation =
                ImportTranslation("test translation", importFileBuilder.data.importLanguages[0]).apply {
                    key = importFileBuilder.data.importKeys.first().self
                }
    }

    class OrganizationBuilder(
            testDataBuilder: TestDataBuilder
    ) : EntityDataBuilder<Organization> {
        override var self: Organization = Organization()
    }
}
