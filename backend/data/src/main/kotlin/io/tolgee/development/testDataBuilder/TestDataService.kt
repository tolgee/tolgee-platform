package io.tolgee.development.testDataBuilder

import io.tolgee.development.testDataBuilder.builders.ImportBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.service.ApiKeyService
import io.tolgee.service.AutoTranslationService
import io.tolgee.service.KeyMetaService
import io.tolgee.service.KeyService
import io.tolgee.service.LanguageService
import io.tolgee.service.OrganizationRoleService
import io.tolgee.service.OrganizationService
import io.tolgee.service.PermissionService
import io.tolgee.service.ProjectService
import io.tolgee.service.ScreenshotService
import io.tolgee.service.TagService
import io.tolgee.service.TranslationCommentService
import io.tolgee.service.TranslationService
import io.tolgee.service.UserAccountService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.machineTranslation.MtCreditBucketService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class TestDataService(
  private val userAccountService: UserAccountService,
  private val projectService: ProjectService,
  private val languageService: LanguageService,
  private val importService: ImportService,
  private val keyService: KeyService,
  private val keyMetaService: KeyMetaService,
  private val translationService: TranslationService,
  private val permissionService: PermissionService,
  private val entityManager: EntityManager,
  private val screenshotService: ScreenshotService,
  private val translationCommentService: TranslationCommentService,
  private val tagService: TagService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val apiKeyService: ApiKeyService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val mtCreditBucketService: MtCreditBucketService,
  private val autoTranslateService: AutoTranslationService
) {
  @Transactional
  fun saveTestData(builder: TestDataBuilder) {
    prepare()

    saveAllUsers(builder)
    saveOrganizationData(builder)
    saveAllMtCreditBuckets(builder)
    saveProjectData(builder)

    finalize()
  }

  private fun saveOrganizationData(builder: TestDataBuilder) {
    saveAllOrganizations(builder)
    saveOrganizationDependants(builder)
  }

  private fun saveProjectData(builder: TestDataBuilder) {
    saveAllProjects(builder)
    saveAllProjectDependants(builder)
  }

  private fun saveOrganizationDependants(builder: TestDataBuilder) {
    saveOrganizationRoles(builder)
    saveOrganizationAvatars(builder)
  }

  private fun saveOrganizationAvatars(builder: TestDataBuilder) {
    builder.data.organizations.forEach { organizationBuilder ->
      organizationBuilder.data.avatarFile?.let { file ->
        organizationService.setAvatar(organizationBuilder.self, file.inputStream)
      }
    }
  }

  private fun saveOrganizationRoles(builder: TestDataBuilder) {
    organizationRoleService.saveAll(builder.data.organizations.flatMap { it.data.roles.map { it.self } })
  }

  private fun finalize() {
    entityManager.flush()
    clearEntityManager()
  }

  private fun prepare() {
    // Sometimes creating data randomly fails,
    // because objects with same ID are already in the session
    clearEntityManager()
  }

  private fun saveAllProjectDependants(builder: TestDataBuilder) {
    savePermissions(builder)
    saveApiKeys(builder)
    saveLanguages(builder)
    saveMtServiceConfigs(builder)
    saveKeyData(builder)
    saveTranslationData(builder)
    saveImportData(builder)
    saveAutoTranslationConfigs(builder)
    saveProjectAvatars(builder)
  }

  private fun saveAutoTranslationConfigs(builder: TestDataBuilder) {
    builder.data.projects.map { it.data.autoTranslationConfigBuilder }.filterNotNull().forEach {
      autoTranslateService.saveConfig(it.self)
    }
  }

  private fun saveProjectAvatars(builder: TestDataBuilder) {
    builder.data.projects.forEach { projectBuilder ->
      projectBuilder.data.avatarFile?.let { file ->
        projectService.setAvatar(projectBuilder.self, file.inputStream)
      }
    }
  }

  private fun saveImportData(builder: TestDataBuilder) {
    val importBuilders = saveImports(builder)
    saveAllImportDependants(importBuilders)
  }

  private fun saveImports(builder: TestDataBuilder): List<ImportBuilder> {
    val importBuilders = builder.data.projects.flatMap { repoBuilder -> repoBuilder.data.imports }
    importService.saveAllImports(importBuilders.map { it.self })
    return importBuilders
  }

  private fun saveTranslationData(builder: TestDataBuilder) {
    val translationBuilders = saveTranslations(builder)
    saveTranslationDependants(translationBuilders)
  }

  private fun saveKeyData(builder: TestDataBuilder) {
    val keyBuilders = saveKeys(builder)
    saveAllKeyDependants(keyBuilders)
  }

  private fun saveTranslationDependants(translationBuilders: List<TranslationBuilder>) {
    val translationComments = translationBuilders.flatMap { it.data.comments.map { it.self } }
    translationCommentService.createAll(translationComments)
  }

  private fun saveTranslations(builder: TestDataBuilder): List<TranslationBuilder> {
    val translationBuilders = builder.data.projects.flatMap { it.data.translations }
    translationService.saveAll(translationBuilders.map { it.self })
    return translationBuilders
  }

  private fun saveKeys(builder: TestDataBuilder): List<KeyBuilder> {
    val keyBuilders = builder.data.projects.flatMap { it.data.keys.map { it } }
    keyService.saveAll(keyBuilders.map { it.self })
    return keyBuilders
  }

  private fun saveMtServiceConfigs(builder: TestDataBuilder) {
    mtServiceConfigService.saveAll(
      builder.data.projects.flatMap {
        it.data.translationServiceConfigs.map { it.self }
      }
    )
  }

  private fun saveLanguages(builder: TestDataBuilder) {
    val languages = builder.data.projects.flatMap { it.data.languages.map { it.self } }
    languageService.saveAll(languages)
  }

  private fun saveApiKeys(builder: TestDataBuilder) {
    apiKeyService.saveAll(builder.data.projects.flatMap { it.data.apiKeys.map { it.self } })
  }

  private fun savePermissions(builder: TestDataBuilder) {
    permissionService.saveAll(builder.data.projects.flatMap { it.data.permissions.map { it.self } })
  }

  private fun saveAllKeyDependants(keyBuilders: List<KeyBuilder>) {
    val metas = keyBuilders.map { it.data.meta?.self }.filterNotNull()
    keyMetaService.saveAll(metas)
    tagService.saveAll(metas.flatMap { it.tags })
    screenshotService.saveAll(keyBuilders.flatMap { it.data.screenshots.map { it.self } }.toList())
  }

  private fun saveAllImportDependants(importBuilders: List<ImportBuilder>) {
    val importFileBuilders = importBuilders.flatMap { it.data.importFiles }
    val importKeyMetas = importFileBuilders.flatMap { it.data.importKeys.map { it.self.keyMeta } }.filterNotNull()
    keyMetaService.saveAll(importKeyMetas)
    keyMetaService.saveAllCodeReferences(importKeyMetas.flatMap { it.codeReferences })
    keyMetaService.saveAllComments(importKeyMetas.flatMap { it.comments })
    importService.saveAllKeys(importFileBuilders.flatMap { it.data.importKeys.map { it.self } })
    val importFiles = importFileBuilders.map { it.self }
    importService.saveAllFiles(importFiles)
    val fileIssues = importFiles.flatMap { it.issues }
    importService.saveAllFileIssues(fileIssues)
    importService.saveAllFileIssueParams(fileIssues.flatMap { it.params ?: emptyList() })
    importService.saveTranslations(importFileBuilders.flatMap { it.data.importTranslations.map { it.self } })
    importService.saveLanguages(importFileBuilders.flatMap { it.data.importLanguages.map { it.self } })
  }

  private fun saveAllProjects(builder: TestDataBuilder) {
    projectService.saveAll(builder.data.projects.map { it.self })
  }

  private fun saveAllMtCreditBuckets(builder: TestDataBuilder) {
    mtCreditBucketService.saveAll(builder.data.mtCreditBuckets.map { it.self })
  }

  private fun saveAllOrganizations(builder: TestDataBuilder) {
    organizationService.saveAll(
      builder.data.organizations.map {
        it.self.apply {
          val slug = this.slug
          if (slug == null || slug.isEmpty()) {
            this.slug = organizationService.generateSlug(this.name!!)
          }
        }
      }
    )
  }

  private fun saveAllUsers(builder: TestDataBuilder) {
    val userAccountBuilders = builder.data.userAccounts
    userAccountService.saveAll(
      userAccountBuilders.map {
        it.self.password = userAccountService.encodePassword(it.rawPassword)
        it.self
      }
    )
    saveUserAvatars(userAccountBuilders)
  }

  private fun saveUserAvatars(userAccountBuilders: MutableList<UserAccountBuilder>) {
    userAccountBuilders.forEach {
      it.data.avatarFile?.let { file ->
        userAccountService.setAvatar(it.self, file.inputStream)
      }
    }
  }

  private fun clearEntityManager() {
    entityManager.clear()
  }

  fun saveTestData(ft: TestDataBuilder.() -> Unit): TestDataBuilder {
    val builder = TestDataBuilder()
    ft(builder)
    saveTestData(builder)
    return builder
  }
}
