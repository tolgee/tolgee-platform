package io.tolgee.development.testDataBuilder

import io.tolgee.activity.ActivityHolder
import io.tolgee.component.eventListeners.LanguageStatsListener
import io.tolgee.development.testDataBuilder.builders.AuthProviderChangeRequestBuilder
import io.tolgee.development.testDataBuilder.builders.BatchJobBuilder
import io.tolgee.development.testDataBuilder.builders.GlossaryBuilder
import io.tolgee.development.testDataBuilder.builders.GlossaryTermBuilder
import io.tolgee.development.testDataBuilder.builders.ImportBuilder
import io.tolgee.development.testDataBuilder.builders.InvitationBuilder
import io.tolgee.development.testDataBuilder.builders.KeyBuilder
import io.tolgee.development.testDataBuilder.builders.PatBuilder
import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.development.testDataBuilder.builders.TestDataBuilder
import io.tolgee.development.testDataBuilder.builders.TranslationBuilder
import io.tolgee.development.testDataBuilder.builders.UserAccountBuilder
import io.tolgee.development.testDataBuilder.builders.UserPreferencesBuilder
import io.tolgee.development.testDataBuilder.builders.slack.SlackUserConnectionBuilder
import io.tolgee.model.Project
import io.tolgee.service.TenantService
import io.tolgee.service.automations.AutomationService
import io.tolgee.service.bigMeta.BigMetaService
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.dataImport.ImportService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.key.KeyMetaService
import io.tolgee.service.key.KeyService
import io.tolgee.service.key.NamespaceService
import io.tolgee.service.key.ScreenshotService
import io.tolgee.service.key.TagService
import io.tolgee.service.language.LanguageService
import io.tolgee.service.machineTranslation.MtServiceConfigService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditBucketService
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.project.LanguageStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.ApiKeyService
import io.tolgee.service.security.AuthProviderChangeService
import io.tolgee.service.security.PatService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserAccountService
import io.tolgee.service.security.UserPreferencesService
import io.tolgee.service.translation.AutoTranslationService
import io.tolgee.service.translation.TranslationCommentService
import io.tolgee.service.translation.TranslationService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional

@Service
class TestDataService(
  private val passwordEncoder: PasswordEncoder,
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
  private val tenantService: TenantService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
  private val apiKeyService: ApiKeyService,
  private val mtServiceConfigService: MtServiceConfigService,
  private val mtCreditBucketService: MtCreditBucketService,
  private val autoTranslateService: AutoTranslationService,
  private val transactionManager: PlatformTransactionManager,
  private val additionalTestDataSavers: List<AdditionalTestDataSaver>,
  private val userPreferencesService: UserPreferencesService,
  private val authProviderChangeService: AuthProviderChangeService,
  private val languageStatsService: LanguageStatsService,
  private val patService: PatService,
  private val notificationService: NotificationService,
  private val namespaceService: NamespaceService,
  private val bigMetaService: BigMetaService,
  private val activityHolder: ActivityHolder,
  private val automationService: AutomationService,
  private val contentDeliveryConfigService: ContentDeliveryConfigService,
  private val languageStatsListener: LanguageStatsListener,
  private val invitationService: InvitationService,
) : Logging {
  @Transactional
  fun saveTestData(ft: TestDataBuilder.() -> Unit): TestDataBuilder {
    val builder = TestDataBuilder()
    ft(builder)
    saveTestData(builder)
    return builder
  }

  @Transactional
  fun saveTestData(builder: TestDataBuilder) {
    activityHolder.enableAutoCompletion = false
    languageStatsListener.bypass = true
    runBeforeSaveMethodsOfAdditionalSavers(builder)
    prepare()

    // Projects have to be stored in separate transaction since projectHolder's
    // project has to be stored for transaction scope.
    //
    // To be able to save project in its separate transaction,
    // user/organization has to be stored first.
    executeInNewTransaction(transactionManager) {
      generateSlugsForOrganizations(builder)
      saveAllUsers(builder)
      saveOrganizationData(builder)
    }

    executeInNewTransaction(transactionManager) {
      additionalTestDataSavers.forEach { it.save(builder) }
    }
    entityManager.flush()
    entityManager.clear()

    executeInNewTransaction(transactionManager) {
      saveProjectData(builder)
      saveGlossaryData(builder)
      saveNotifications(builder)
      finalize()
    }

    updateLanguageStats(builder)
    activityHolder.enableAutoCompletion = true
    languageStatsListener.bypass = false

    runAfterSaveMethodsOfAdditionalSavers(builder)
  }

  private fun generateSlugsForOrganizations(builder: TestDataBuilder) {
    builder.data.organizations.forEach {
      val organization = it.self
      val slug = organization.slug
      if (slug.isEmpty()) {
        organization.slug = organizationService.generateSlug(organization.name)
      }
    }
  }

  @Transactional
  fun cleanTestData(builder: TestDataBuilder) {
    runBeforeCleanMethodsOfAdditionalSavers(builder)
    tryUntilItDoesntBreakConstraint {
      executeInNewTransaction(transactionManager) {
        builder.data.userAccounts.forEach {
          userAccountService.findActive(it.self.username)?.let { user ->
            notificationService.deleteNotificationsOfUser(user.id)
            userAccountService.delete(user)
          }
        }

        builder.data.organizations.forEach { organizationBuilder ->
          organizationBuilder.self.name.let { name ->
            organizationService.findAllByName(name).forEach { org ->
              organizationService.delete(org)
            }
          }
        }
      }
    }

    additionalTestDataSavers.forEach { dataSaver ->
      tryUntilItDoesntBreakConstraint {
        executeInNewTransaction(transactionManager) {
          entityManager.createNativeQuery("set statement_timeout to 1000000;").executeUpdate()
          dataSaver.clean(builder)
        }
      }
    }
    runAfterCleanMethodsOfAdditionalSavers(builder)
  }

  private fun updateLanguageStats(builder: TestDataBuilder) {
    builder.data.projects.forEach {
      try {
        executeInNewTransaction(transactionManager) { _ ->
          languageStatsService.refreshLanguageStats(it.self.id)
          entityManager.flush()
        }
      } catch (e: DataIntegrityViolationException) {
        logger.info(e.stackTraceToString())
      }
    }
  }

  private fun saveOrganizationData(builder: TestDataBuilder) {
    saveAllOrganizations(builder)
    saveOrganizationDependants(builder)
  }

  private fun saveProjectData(builder: TestDataBuilder) {
    saveAllProjects(builder)
  }

  private fun saveOrganizationDependants(builder: TestDataBuilder) {
    saveOrganizationInvitations(builder)
    saveOrganizationRoles(builder)
    saveOrganizationAvatars(builder)
    saveAllMtCreditBuckets(builder)
    saveSlackWorkspaces(builder)
    saveOrganizationTenants(builder)
    saveLlmProviders(builder)
  }

  private fun saveLlmProviders(builder: TestDataBuilder) {
    builder.data.organizations.forEach { organizationBuilder ->
      organizationBuilder.data.llmProviders.map { it.self }.forEach {
        entityManager.persist(it)
      }
    }
  }

  private fun saveSlackWorkspaces(builder: TestDataBuilder) {
    builder.data.organizations.forEach { organizationBuilder ->
      organizationBuilder.data.slackWorkspaces.map { it.self }.forEach {
        entityManager.persist(it)
      }
    }
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

  private fun saveOrganizationTenants(builder: TestDataBuilder) {
    tenantService.saveAll(builder.data.organizations.mapNotNull { it.data.tenant?.self })
  }

  private fun saveGlossaryData(builder: TestDataBuilder) {
    val builders = saveGlossaries(builder)
    saveGlossariesDependants(builders)
  }

  private fun saveGlossaries(builder: TestDataBuilder): List<GlossaryBuilder> {
    val builders = builder.data.organizations.flatMap { it.data.glossaries }
    builders.forEach {
      entityManager.persist(it.self)
    }
    return builders
  }

  private fun saveGlossariesDependants(builders: List<GlossaryBuilder>) {
    saveGlossaryTermData(builders)
  }

  private fun saveGlossaryTermData(builders: List<GlossaryBuilder>) {
    val builders = saveGlossaryTerms(builders)
    saveGlossaryTermsDependants(builders)
  }

  private fun saveGlossaryTerms(builders: List<GlossaryBuilder>): List<GlossaryTermBuilder> {
    val builders = builders.flatMap { it.data.terms }
    builders.forEach {
      entityManager.persist(it.self)
    }
    return builders
  }

  private fun saveGlossaryTermsDependants(builders: List<GlossaryTermBuilder>) {
    saveGlossaryTranslations(builders)
  }

  private fun saveGlossaryTranslations(builders: List<GlossaryTermBuilder>) {
    val builders = builders.flatMap { it.data.translations }
    builders.forEach {
      entityManager.persist(it.self)
    }
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

  private fun saveAllProjectDependants(builder: ProjectBuilder) {
    saveApiKeys(builder)
    saveLanguages(builder)
    saveProjectInvitations(builder.testDataBuilder.data.invitations, builder.self)
    savePermissions(builder)
    saveMtServiceConfigs(builder)
    saveAllNamespaces(builder)
    saveKeyData(builder)
    saveTranslationData(builder)
    saveImportData(builder)
    saveAutoTranslationConfigs(builder)
    saveProjectAvatars(builder)
    saveScreenshotData(builder)
    saveKeyDistances(builder)
    saveContentStorages(builder)
    saveContentDeliveryConfigs(builder)
    saveWebhookConfigs(builder)
    saveSlackConfigs(builder)
    saveAutomations(builder)
    saveImportSettings(builder)
    saveBatchJobs(builder)
    saveTasks(builder)
    saveTaskKeys(builder)
    savePrompts(builder)
    saveAiPlaygroundResults(builder)
    saveLabels(builder)
    saveSuggestions(builder)
  }

  private fun saveImportSettings(builder: ProjectBuilder) {
    builder.data.importSettings?.let {
      entityManager.merge(it.userAccount)
      entityManager.persist(it)
    }
  }

  private fun saveWebhookConfigs(builder: ProjectBuilder) {
    builder.data.webhookConfigs.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveSlackConfigs(builder: ProjectBuilder) {
    builder.data.slackConfigs.forEach {
      entityManager.persist(it.self)
    }

    builder.data.slackConfigs.forEach { slackConfig ->
      val messages =
        slackConfig.data.slackMessages
          .map { it.self }
          .toMutableList()
      messages.forEach { entityManager.persist(it) }
    }
  }

  private fun saveContentDeliveryConfigs(builder: ProjectBuilder) {
    builder.data.contentDeliveryConfigs.forEach {
      if (it.self.slug.isEmpty()) {
        it.self.slug = contentDeliveryConfigService.generateSlug()
      }
      entityManager.persist(it.self)
    }
  }

  private fun saveContentStorages(builder: ProjectBuilder) {
    builder.data.contentStorages.forEach {
      it.self.azureContentStorageConfig?.let { entityManager.persist(it) }
      it.self.s3ContentStorageConfig?.let { entityManager.persist(it) }
      entityManager.persist(it.self)
    }
  }

  private fun saveAutomations(builder: ProjectBuilder) {
    builder.data.automations.forEach {
      it.self.actions.forEach {
        entityManager.persist(it)
      }
      it.self.triggers.forEach { entityManager.persist(it) }
      automationService.save(it.self)
    }
  }

  private fun saveKeyDistances(builder: ProjectBuilder) {
    builder.data.keyDistances.forEach {
      it.self.key1Id = it.key1.id
      it.self.key2Id = it.key2.id
      bigMetaService.saveKeyDistance(it.self)
    }
  }

  private fun saveScreenshotData(builder: ProjectBuilder) {
    val screenshotBuilders = builder.data.screenshots
    screenshotService.saveAll(screenshotBuilders.map { it.self })
    screenshotBuilders.forEach {
      screenshotService.storeFiles(
        it.self,
        it.image?.toByteArray(),
        it.middleSized?.toByteArray(),
        it.thumbnail?.toByteArray(),
      )
    }
    screenshotService.saveAllReferences(builder.data.keyScreenshotReferences.map { it.self })
  }

  private fun saveAllNamespaces(builder: ProjectBuilder) {
    builder.data.namespaces.forEach {
      namespaceService.save(it.self)
    }
  }

  private fun saveAutoTranslationConfigs(builder: ProjectBuilder) {
    builder.data.autoTranslationConfigBuilders.forEach {
      autoTranslateService.saveConfig(it.self)
    }
  }

  private fun saveProjectAvatars(builder: ProjectBuilder) {
    builder.data.avatarFile?.let { file ->
      projectService.setAvatar(builder.self, file.inputStream)
    }
  }

  private fun saveImportData(builder: ProjectBuilder) {
    val importBuilders = saveImports(builder)
    saveAllImportDependants(importBuilders)
  }

  private fun saveImports(builder: ProjectBuilder): List<ImportBuilder> {
    val importBuilders = builder.data.imports
    importService.saveAllImports(importBuilders.map { it.self })
    return importBuilders
  }

  private fun saveTranslationData(builder: ProjectBuilder) {
    val translationBuilders = saveTranslations(builder)
    saveTranslationDependants(translationBuilders)
  }

  private fun saveKeyData(builder: ProjectBuilder) {
    val keyBuilders = saveKeys(builder)
    saveAllKeyDependants(keyBuilders)
  }

  private fun saveTranslationDependants(translationBuilders: List<TranslationBuilder>) {
    val translationComments = translationBuilders.flatMap { it.data.comments.map { it.self } }
    translationCommentService.saveAll(translationComments)
  }

  private fun saveTranslations(builder: ProjectBuilder): List<TranslationBuilder> {
    val translationBuilders = builder.data.translations
    translationService.saveAll(translationBuilders.map { it.self })
    return translationBuilders
  }

  private fun saveKeys(builder: ProjectBuilder): List<KeyBuilder> {
    val keyBuilders = builder.data.keys.map { it }
    keyService.saveAll(keyBuilders.map { it.self })
    return keyBuilders
  }

  private fun saveMtServiceConfigs(builder: ProjectBuilder) {
    mtServiceConfigService.saveAll(
      builder.data.translationServiceConfigs.map { it.self },
    )
  }

  private fun saveLanguages(builder: ProjectBuilder) {
    val languages =
      builder.data.languages.map {
        // refresh entity if updating to get new stats
        if (it.self.id != 0L) languageService.getEntity(it.self.id) else it.self
      }
    languageService.saveAll(languages)
  }

  private fun saveApiKeys(builder: ProjectBuilder) {
    builder.data.apiKeys.forEach { apiKeyService.save(it.self) }
  }

  private fun savePermissions(builder: ProjectBuilder) {
    val toSave = builder.data.permissions.map { it.self }
    permissionService.saveAll(toSave)
  }

  private fun saveAllKeyDependants(keyBuilders: List<KeyBuilder>) {
    val metas = keyBuilders.map { it.data.meta?.self }.filterNotNull()
    tagService.saveAll(metas.flatMap { it.tags })
    keyMetaService.saveAll(metas)
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
    val params = fileIssues.flatMap { it.params }
    importService.saveAllFileIssueParams(params)
    importService.saveTranslations(importFileBuilders.flatMap { it.data.importTranslations.map { it.self } })
    importService.saveLanguages(importFileBuilders.flatMap { it.data.importLanguages.map { it.self } })
  }

  private fun saveAllProjects(builder: TestDataBuilder) {
    val projectBuilders = builder.data.projects
    projectBuilders.forEach { projectBuilder ->
      projectService.save(projectBuilder.self)
      saveAllProjectDependants(projectBuilder)
    }
  }

  private fun saveAllMtCreditBuckets(builder: TestDataBuilder) {
    mtCreditBucketService.saveAll(builder.data.mtCreditBuckets.map { it.self })
  }

  private fun saveAllOrganizations(builder: TestDataBuilder) {
    val organizationsToSave = builder.data.organizations.map { it.self }

    organizationsToSave.forEach { org ->
      permissionService.save(org.basePermission)
      organizationService.save(org)
    }
  }

  private fun saveAllUsers(builder: TestDataBuilder) {
    val userAccountBuilders = builder.data.userAccounts
    userAccountBuilders.forEach { userBuilder ->
      userBuilder.self.password = encodePassword(userBuilder.rawPassword)
    }
    userAccountService.saveAll(userAccountBuilders.map { it.self })
    saveUserAvatars(userAccountBuilders)
    saveUserPreferences(userAccountBuilders.mapNotNull { it.data.userPreferences })
    saveAuthProviderChangeRequests(userAccountBuilders.mapNotNull { it.data.authProviderChangeRequest })
    saveUserPats(userAccountBuilders.flatMap { it.data.pats })
    saveUserSlackConnections(userAccountBuilders.flatMap { it.data.slackUserConnections })
  }

  private fun saveUserPats(data: List<PatBuilder>) {
    data.forEach { patService.save(it.self) }
  }

  private fun saveUserSlackConnections(data: List<SlackUserConnectionBuilder>) {
    data.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveNotifications(builder: TestDataBuilder) {
    builder.data.userAccounts
      .flatMap { it.data.notifications }
      .sortedBy { it.self.linkedTask?.name }
      .forEach {
        notificationService.notify(it.self)
      }
  }

  private fun saveUserPreferences(data: List<UserPreferencesBuilder>) {
    data.forEach { userPreferencesService.save(it.self) }
  }

  private fun saveAuthProviderChangeRequests(data: List<AuthProviderChangeRequestBuilder>) {
    data.forEach { authProviderChangeService.save(it.self) }
  }

  private fun saveUserAvatars(userAccountBuilders: MutableList<UserAccountBuilder>) {
    userAccountBuilders.forEach {
      it.data.avatarFile?.let { file ->
        userAccountService.setAvatar(it.self, file.inputStream)
      }
    }
  }

  private fun saveBatchJobs(builder: ProjectBuilder) {
    builder.data.batchJobs.forEach {
      it.targetProvider?.let { provider ->
        it.self.target = provider()
      }
      entityManager.persist(it.self)
      saveChunkExecutions(it)
    }
  }

  private fun saveTasks(builder: ProjectBuilder) {
    builder.data.tasks.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveTaskKeys(builder: ProjectBuilder) {
    builder.data.taskKeys.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun savePrompts(builder: ProjectBuilder) {
    builder.data.prompts.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveAiPlaygroundResults(builder: ProjectBuilder) {
    builder.data.aiPlaygroundResults.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveLabels(builder: ProjectBuilder) {
    builder.data.labels.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveSuggestions(builder: ProjectBuilder) {
    builder.data.suggestions.forEach {
      entityManager.persist(it.self)
    }
  }

  private fun saveChunkExecutions(batchJobBuilder: BatchJobBuilder) {
    batchJobBuilder.data.chunkExecutions.forEach {
      entityManager.persist(it.self)
      it.successfulTargetsProvider?.let { provider ->
        it.self.successTargets = provider()
      }
    }
  }

  private fun clearEntityManager() {
    entityManager.clear()
  }

  private fun encodePassword(rawPassword: String?): String? {
    rawPassword ?: return null
    return passwordHashCache.computeIfAbsent(rawPassword) {
      passwordEncoder.encode(rawPassword)
    }
  }

  companion object {
    private val passwordHashCache = mutableMapOf<String, String>()
  }

  private fun runBeforeSaveMethodsOfAdditionalSavers(builder: TestDataBuilder) {
    executeInNewTransaction(transactionManager) {
      additionalTestDataSavers.forEach {
        it.beforeSave(builder)
      }
    }
  }

  private fun runAfterSaveMethodsOfAdditionalSavers(builder: TestDataBuilder) {
    executeInNewTransaction(transactionManager) {
      additionalTestDataSavers.forEach {
        it.afterSave(builder)
      }
    }
  }

  private fun runBeforeCleanMethodsOfAdditionalSavers(builder: TestDataBuilder) {
    executeInNewTransaction(transactionManager) {
      additionalTestDataSavers.forEach {
        it.beforeClean(builder)
      }
    }
  }

  private fun runAfterCleanMethodsOfAdditionalSavers(builder: TestDataBuilder) {
    executeInNewTransaction(transactionManager) {
      additionalTestDataSavers.forEach {
        it.afterClean(builder)
      }
    }
  }

  private fun saveOrganizationInvitations(builder: TestDataBuilder) {
    builder.data.invitations.filter { it.self.organizationRole != null }.forEach {
      invitationService.save(it.self)
    }
  }

  private fun saveProjectInvitations(
    invitations: MutableList<InvitationBuilder>,
    self: Project,
  ) {
    invitations.filter { it.self.permission?.project == self }.forEach {
      invitationService.save(it.self)
    }
  }
}
