package io.tolgee.development.testDataBuilder

import io.tolgee.model.*
import io.tolgee.model.dataImport.*
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta

typealias FT<T> = T.() -> Unit

class DataBuilders {
    class ProjectBuilder(userOwner: UserAccount? = null,
                         organizationOwner: Organization? = null,
                         val testDataBuilder: TestDataBuilder
    ) : BaseEntityDataBuilder<Project>() {
        override var self: Project = Project().apply {
            if (userOwner == null && organizationOwner == null) {
                if (testDataBuilder.data.userAccounts.size > 0) {
                    this.userOwner = testDataBuilder.data.userAccounts.first().self
                } else if (testDataBuilder.data.organizations.size > 0) {
                    this.organizationOwner = testDataBuilder.data.organizations.first().self
                }
                return@apply
            }

            this.userOwner = userOwner
            this.organizationOwner = organizationOwner
        }

        class DATA {
            val permissions = mutableListOf<PermissionBuilder>()
            val languages = mutableListOf<LanguageBuilder>()
            val imports = mutableListOf<ImportBuilder>()
            val keys = mutableListOf<KeyBuilder>()
            val translations = mutableListOf<TranslationBuilder>()
        }

        var data = DATA()

        fun addPermission(ft: FT<PermissionBuilder>) = addOperation(data.permissions, ft)

        fun addImport(author: UserAccount? = null, ft: FT<ImportBuilder>) =
                addOperation(data.imports, ImportBuilder(this, author), ft)

        fun addLanguage(ft: FT<LanguageBuilder>) =
                addOperation(data.languages, ft)

        fun addKey(ft: FT<KeyBuilder>) = addOperation(data.keys, ft)

        fun addTranslation(ft: FT<TranslationBuilder>) = addOperation(data.translations, ft)

    }

    class ImportBuilder(
            val projectBuilder: ProjectBuilder,
            author: UserAccount? = null
    ) : BaseEntityDataBuilder<Import>() {
        class DATA {
            val importFiles = mutableListOf<ImportFileBuilder>()
        }

        val data = DATA()

        override var self: Import = Import(author ?: projectBuilder.self.userOwner!!, projectBuilder.self)

        fun addImportFile(ft: FT<ImportFileBuilder>) = addOperation(data.importFiles, ft)
    }

    class ImportFileBuilder(importBuilder: ImportBuilder) : BaseEntityDataBuilder<ImportFile>() {
        override var self: ImportFile = ImportFile("lang.json", importBuilder.self)

        class DATA {
            val importKeys = mutableListOf<ImportKeyBuilder>()
            val importLanguages = mutableListOf<ImportLanguageBuilder>()
            val importTranslations = mutableListOf<ImportTranslationBuilder>()
        }

        val data = DATA()

        fun addImportKey(ft: FT<ImportKeyBuilder>) = addOperation(data.importKeys, ft).also {
            it.self {
                this@ImportFileBuilder.self.keys.add(this)
                this.files.add(this@ImportFileBuilder.self)
            }
        }

        fun addImportLanguage(ft: FT<ImportLanguageBuilder>) = addOperation(data.importLanguages, ft).also {
            it.self { this.file = this@ImportFileBuilder.self }
        }

        fun addImportTranslation(ft: FT<ImportTranslationBuilder>) = addOperation(data.importTranslations, ft)
    }

    class ImportKeyBuilder(
            importFileBuilder: ImportFileBuilder
    ) : EntityDataBuilder<ImportKey> {

        class DATA {
            var meta: KeyMetaBuilder? = null
            val screenshots = mutableListOf<ScreenshotBuilder>()
        }

        val data = DATA()

        override var self: ImportKey = ImportKey("testKey")
                .also { it.files.add(importFileBuilder.self) }

        fun addMeta(ft: FT<KeyMetaBuilder>) {
            data.meta = KeyMetaBuilder(this).apply(ft)
        }
    }

    class KeyMetaBuilder(
            importKeyBuilder: ImportKeyBuilder? = null,
            keyBuilder: KeyBuilder? = null
    ) : EntityDataBuilder<KeyMeta> {
        override var self: KeyMeta = KeyMeta(
                key = keyBuilder?.self,
                importKey = importKeyBuilder?.self
        ).also {
            keyBuilder?.self {
                keyMeta = it
            }
            importKeyBuilder?.self {
                keyMeta = it
            }
        }
    }

    class ScreenshotBuilder(
            keyBuilder: KeyBuilder
    ) : EntityDataBuilder<Screenshot> {
        override var self: Screenshot = Screenshot().also {
            it.key = keyBuilder.self
            keyBuilder.self {
                screenshots.add(it)
            }
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
                ImportTranslation("test translation", importFileBuilder.data.importLanguages[0].self).apply {
                    key = importFileBuilder.data.importKeys.first().self
                }
    }

    class OrganizationBuilder(
            val testDataBuilder: TestDataBuilder
    ) : EntityDataBuilder<Organization> {
        override var self: Organization = Organization()
    }

    class KeyBuilder(
            val projectBuilder: ProjectBuilder
    ) : BaseEntityDataBuilder<Key>() {

        class DATA {
            var meta: KeyMetaBuilder? = null
            var screenshots = mutableListOf<ScreenshotBuilder>()
        }

        val data = DATA()

        override var self: Key = Key().also {
            it.project = projectBuilder.self
        }

        fun addMeta(ft: FT<KeyMetaBuilder>) {
            data.meta = KeyMetaBuilder(keyBuilder = this).apply(ft)
        }

        fun addScreenshot(ft: FT<ScreenshotBuilder>) = addOperation(data.screenshots, ft)
    }

    class LanguageBuilder(
            val projectBuilder: ProjectBuilder
    ) : EntityDataBuilder<Language> {
        override var self: Language = Language().apply {
            project = projectBuilder.self
        }
    }

    class TranslationBuilder(
            val projectBuilder: ProjectBuilder
    ) : EntityDataBuilder<Translation> {
        override var self: Translation = Translation().apply { text = "What a text" }
    }

    class UserAccountBuilder(
            val testDataBuilder: TestDataBuilder
    ) : EntityDataBuilder<UserAccount> {
        var rawPassword = "admin"
        override var self: UserAccount = UserAccount()
    }

    class PermissionBuilder(
            val projectBuilder: ProjectBuilder
    ) : EntityDataBuilder<Permission> {
        override var self: Permission = Permission()
    }
}
