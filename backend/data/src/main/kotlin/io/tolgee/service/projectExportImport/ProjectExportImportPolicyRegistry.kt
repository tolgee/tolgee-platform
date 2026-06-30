package io.tolgee.service.projectExportImport

import io.tolgee.model.AiPlaygroundResult
import io.tolgee.model.ApiKey
import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.AutoTranslationConfig
import io.tolgee.model.DismissedAnnouncement
import io.tolgee.model.EmailVerification
import io.tolgee.model.ForcedServerDateTime
import io.tolgee.model.InstanceId
import io.tolgee.model.Invitation
import io.tolgee.model.Language
import io.tolgee.model.LanguageStats
import io.tolgee.model.LlmProvider
import io.tolgee.model.MtCreditBucket
import io.tolgee.model.Organization
import io.tolgee.model.OrganizationRole
import io.tolgee.model.Pat
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.Prompt
import io.tolgee.model.QuickStart
import io.tolgee.model.Screenshot
import io.tolgee.model.SsoTenant
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.UploadedImage
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.model.activity.ActivityDescribingEntity
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.activity.ActivityRevision
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.batch.BatchJobChunkExecution
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.BranchMergeChange
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.contentDelivery.AzureContentStorageConfig
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.contentDelivery.S3ContentStorageConfig
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.model.dataImport.ImportKey
import io.tolgee.model.dataImport.ImportLanguage
import io.tolgee.model.dataImport.ImportSettings
import io.tolgee.model.dataImport.ImportTranslation
import io.tolgee.model.dataImport.issues.ImportFileIssue
import io.tolgee.model.dataImport.issues.ImportFileIssueParam
import io.tolgee.model.glossary.Glossary
import io.tolgee.model.glossary.GlossaryTerm
import io.tolgee.model.glossary.GlossaryTermTranslation
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyCodeReference
import io.tolgee.model.key.KeyComment
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.keyBigMeta.KeysDistance
import io.tolgee.model.mtServiceConfig.MtServiceConfig
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationSetting
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackConfigPreference
import io.tolgee.model.slackIntegration.SlackMessageInfo
import io.tolgee.model.slackIntegration.SlackUserConnection
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.temp.UnsuccessfulJobKey
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import io.tolgee.model.translationAgency.TranslationAgency
import io.tolgee.model.translationMemory.TranslationMemory
import io.tolgee.model.translationMemory.TranslationMemoryEntry
import io.tolgee.model.translationMemory.TranslationMemoryProject
import io.tolgee.model.webhook.WebhookConfig
import kotlin.reflect.KClass

/**
 * The single place where every `@Entity` is classified for project export/import.
 * Completeness and freshness are enforced at build time by `ProjectExportImportPolicyGuardTest`.
 */
object ProjectExportImportPolicyRegistry {
  private val policies: Map<String, ExportImportPolicy> =
    buildMap {
      owned(
        Language::class,
        Namespace::class,
        Key::class,
        KeyMeta::class,
        KeyComment::class,
        KeyCodeReference::class,
        Translation::class,
        TranslationComment::class,
        TranslationSuggestion::class,
        Tag::class,
        Label::class,
        Screenshot::class,
        KeyScreenshotReference::class,
        Branch::class,
        Task::class,
        TaskKey::class,
        ProjectQaConfig::class,
        LanguageQaConfig::class,
        TranslationQaIssue::class,
      )

      classify(UserAccount::class, ExportImportPolicy.USER_REF)
      classify(Project::class, ExportImportPolicy.PROJECT_ROOT)

      ignored(
        Organization::class,
        OrganizationRole::class,
        Permission::class,
        ApiKey::class,
        Invitation::class,
        Pat::class,
        UserPreferences::class,
        EmailVerification::class,
        SsoTenant::class,
        AuthProviderChangeRequest::class,
        DismissedAnnouncement::class,
        QuickStart::class,
        ActivityRevision::class,
        ActivityModifiedEntity::class,
        ActivityDescribingEntity::class,
        BranchMerge::class,
        BranchMergeChange::class,
        KeySnapshot::class,
        TranslationSnapshot::class,
        KeyMetaSnapshot::class,
        LanguageStats::class,
        KeysDistance::class,
        AutoTranslationConfig::class,
        MtServiceConfig::class,
        Prompt::class,
        AiPlaygroundResult::class,
        MtCreditBucket::class,
        LlmProvider::class,
        BatchJob::class,
        BatchJobChunkExecution::class,
        UnsuccessfulJobKey::class,
        Import::class,
        ImportFile::class,
        ImportLanguage::class,
        ImportKey::class,
        ImportTranslation::class,
        ImportSettings::class,
        ImportFileIssue::class,
        ImportFileIssueParam::class,
        ContentStorage::class,
        ContentDeliveryConfig::class,
        S3ContentStorageConfig::class,
        AzureContentStorageConfig::class,
        Automation::class,
        AutomationTrigger::class,
        AutomationAction::class,
        WebhookConfig::class,
        SlackConfig::class,
        SlackConfigPreference::class,
        SavedSlackMessage::class,
        SlackMessageInfo::class,
        SlackUserConnection::class,
        OrganizationSlackWorkspace::class,
        Glossary::class,
        GlossaryTerm::class,
        GlossaryTermTranslation::class,
        TranslationMemory::class,
        TranslationMemoryEntry::class,
        TranslationMemoryProject::class,
        Notification::class,
        NotificationSetting::class,
        UploadedImage::class,
        TranslationAgency::class,
        InstanceId::class,
        ForcedServerDateTime::class,
      )
      // Classified by string, not ::class: these live in an EE module :data cannot depend on, so they
      // are unreferenceable here. The `no stale entries` guard validates the names against the live
      // metamodel, so a rename or move still fails the build.
      ignoredByName(
        "io.tolgee.ee.model.EeSubscription",
        "io.tolgee.ee.model.UsageToReport",
      )
    }

  private fun MutableMap<String, ExportImportPolicy>.owned(vararg classes: KClass<*>) =
    classes.forEach { classify(it, ExportImportPolicy.OWNED) }

  private fun MutableMap<String, ExportImportPolicy>.ignored(vararg classes: KClass<*>) =
    classes.forEach { classify(it, ExportImportPolicy.IGNORED) }

  private fun MutableMap<String, ExportImportPolicy>.ignoredByName(vararg classNames: String) =
    classNames.forEach { classifyUnique(it, ExportImportPolicy.IGNORED) }

  private fun MutableMap<String, ExportImportPolicy>.classify(
    klass: KClass<*>,
    policy: ExportImportPolicy,
    // Key by the JVM binary name (java.name) to match every lookup, which uses EntityType.javaType.name
    // — qualifiedName differs for a nested @Entity (`Outer.Inner` vs `Outer$Inner`).
  ) = classifyUnique(klass.java.name, policy)

  fun policyOf(entityClassName: String): ExportImportPolicy? = policies[entityClassName]

  val ownedClassNames: Set<String>
    get() = policies.filterValues { it == ExportImportPolicy.OWNED }.keys

  fun unclassified(managedEntityClassNames: Set<String>): Set<String> = managedEntityClassNames - policies.keys

  fun staleEntries(managedEntityClassNames: Set<String>): Set<String> = policies.keys - managedEntityClassNames

  /**
   * Validate one association of an OWNED entity pointing at [targetClassName]. [droppable] is true
   * when the reference can be dropped on import without breaking the schema — a collection/inverse
   * association or a nullable singular FK. Returns a human-readable violation, or null when allowed.
   */
  fun associationViolation(
    targetClassName: String,
    droppable: Boolean,
  ): String? {
    val policy = policyOf(targetClassName) ?: return "target type $targetClassName is not classified"
    if (policy == ExportImportPolicy.IGNORED && !droppable) {
      return "non-nullable association to IGNORED type $targetClassName cannot be satisfied on import"
    }
    return null
  }
}

internal fun MutableMap<String, ExportImportPolicy>.classifyUnique(
  className: String,
  policy: ExportImportPolicy,
) {
  val previous = put(className, policy)
  require(previous == null) { "$className is classified more than once ($previous and $policy)" }
}
