package io.tolgee.development.testDataBuilder

import io.tolgee.model.ApiKey
import io.tolgee.model.Language
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.MtServiceConfig
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.UserAccount
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

typealias FT<T> = T.() -> Unit

class DataBuilders {
  class ProjectBuilder(
    userOwner: UserAccount? = null,
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
      val apiKeys = mutableListOf<ApiKeyBuilder>()
      val translationServiceConfigs = mutableListOf<MtServiceConfigBuilder>()
    }

    var data = DATA()

    fun addPermission(ft: FT<PermissionBuilder>) = addOperation(data.permissions, ft)

    fun addApiKey(ft: FT<ApiKeyBuilder>) = addOperation(data.apiKeys, ft)

    fun addImport(author: UserAccount? = null, ft: FT<ImportBuilder>) =
      addOperation(data.imports, ImportBuilder(this, author), ft)

    fun addLanguage(ft: FT<LanguageBuilder>) =
      addOperation(data.languages, ft)

    fun addKey(ft: FT<KeyBuilder>) = addOperation(data.keys, ft).also { it.self { project = this@ProjectBuilder.self } }

    fun addTranslation(ft: FT<TranslationBuilder>) = addOperation(data.translations, ft)

    fun addMtServiceConfig(ft: FT<MtServiceConfigBuilder>) =
      addOperation(data.translationServiceConfigs, ft)
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
  ) : BaseEntityDataBuilder<Organization>() {
    class DATA {
      var roles: MutableList<OrganizationRoleBuilder> = mutableListOf()
    }

    val data = DATA()

    fun addRole(ft: FT<OrganizationRoleBuilder>) = addOperation(data.roles, ft)

    fun addMtCreditBucket(ft: FT<MtCreditBucketBuilder>): MtCreditBucketBuilder {
      val builder = MtCreditBucketBuilder()
      testDataBuilder.data.mtCreditBuckets.add(builder)
      builder.self.organization = this@OrganizationBuilder.self
      ft(builder)
      return builder
    }

    override var self: Organization = Organization()
  }

  class OrganizationRoleBuilder(
    val organizationBuilder: OrganizationBuilder
  ) : EntityDataBuilder<OrganizationRole> {

    override var self: OrganizationRole = OrganizationRole().apply {
      organization = organizationBuilder.self
    }
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
  ) : BaseEntityDataBuilder<Translation>() {

    class DATA {
      var comments = mutableListOf<TranslationCommentBuilder>()
    }

    val data = DATA()

    override var self: Translation = Translation().apply { text = "What a text" }

    fun addComment(ft: FT<TranslationCommentBuilder>) = addOperation(data.comments, ft)
  }

  class TranslationCommentBuilder(
    val translationBuilder: TranslationBuilder
  ) : BaseEntityDataBuilder<TranslationComment>() {
    override var self: TranslationComment = TranslationComment(
      translation = translationBuilder.self,
    ).also { comment ->
      translationBuilder.self.key?.project?.userOwner?.let {
        comment.author = it
      }
    }
  }

  class UserAccountBuilder(
    val testDataBuilder: TestDataBuilder
  ) : EntityDataBuilder<UserAccount> {
    var rawPassword = "admin"
    override var self: UserAccount = UserAccount()

    fun addMtCreditBucket(ft: FT<MtCreditBucketBuilder>): MtCreditBucketBuilder {
      val builder = MtCreditBucketBuilder()
      testDataBuilder.data.mtCreditBuckets.add(builder)
      builder.self.userAccount = this.self
      ft(builder)
      return builder
    }
  }

  class PermissionBuilder(
    val projectBuilder: ProjectBuilder
  ) : EntityDataBuilder<Permission> {
    override var self: Permission = Permission().apply {
      project = projectBuilder.self
    }
  }

  class ApiKeyBuilder(
    val projectBuilder: ProjectBuilder
  ) : EntityDataBuilder<ApiKey> {
    override var self: ApiKey = ApiKey(
      "test_api_key", mutableSetOf()
    ).apply {
      project = projectBuilder.self
      projectBuilder.self.userOwner?.let {
        this.userAccount = it
      }
    }
  }

  class MtServiceConfigBuilder(
    val projectBuilder: ProjectBuilder
  ) : BaseEntityDataBuilder<MtServiceConfig>() {
    override var self: MtServiceConfig = MtServiceConfig()
      .apply {
        project = projectBuilder.self
      }
  }

  class MtCreditBucketBuilder() : BaseEntityDataBuilder<MtCreditBucket>() {
    override var self: MtCreditBucket = MtCreditBucket()
  }
}
