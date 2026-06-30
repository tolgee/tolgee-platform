package io.tolgee.service.projectExportImport

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.Screenshot
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyCodeReference
import io.tolgee.model.key.KeyComment
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
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
        "io.tolgee.model.Organization",
        "io.tolgee.model.OrganizationRole",
        "io.tolgee.model.Permission",
        "io.tolgee.model.ApiKey",
        "io.tolgee.model.Invitation",
        "io.tolgee.model.Pat",
        "io.tolgee.model.UserPreferences",
        "io.tolgee.model.EmailVerification",
        "io.tolgee.model.SsoTenant",
        "io.tolgee.model.AuthProviderChangeRequest",
        "io.tolgee.model.DismissedAnnouncement",
        "io.tolgee.model.QuickStart",
        "io.tolgee.model.activity.ActivityRevision",
        "io.tolgee.model.activity.ActivityModifiedEntity",
        "io.tolgee.model.activity.ActivityDescribingEntity",
        "io.tolgee.model.branching.BranchMerge",
        "io.tolgee.model.branching.BranchMergeChange",
        "io.tolgee.model.branching.snapshot.KeySnapshot",
        "io.tolgee.model.branching.snapshot.TranslationSnapshot",
        "io.tolgee.model.branching.snapshot.KeyMetaSnapshot",
        "io.tolgee.model.LanguageStats",
        "io.tolgee.model.keyBigMeta.KeysDistance",
        "io.tolgee.model.AutoTranslationConfig",
        "io.tolgee.model.mtServiceConfig.MtServiceConfig",
        "io.tolgee.model.Prompt",
        "io.tolgee.model.AiPlaygroundResult",
        "io.tolgee.model.MtCreditBucket",
        "io.tolgee.model.LlmProvider",
        "io.tolgee.model.batch.BatchJob",
        "io.tolgee.model.batch.BatchJobChunkExecution",
        "io.tolgee.model.temp.UnsuccessfulJobKey",
        "io.tolgee.model.dataImport.Import",
        "io.tolgee.model.dataImport.ImportFile",
        "io.tolgee.model.dataImport.ImportLanguage",
        "io.tolgee.model.dataImport.ImportKey",
        "io.tolgee.model.dataImport.ImportTranslation",
        "io.tolgee.model.dataImport.ImportSettings",
        "io.tolgee.model.dataImport.issues.ImportFileIssue",
        "io.tolgee.model.dataImport.issues.ImportFileIssueParam",
        "io.tolgee.model.contentDelivery.ContentStorage",
        "io.tolgee.model.contentDelivery.ContentDeliveryConfig",
        "io.tolgee.model.contentDelivery.S3ContentStorageConfig",
        "io.tolgee.model.contentDelivery.AzureContentStorageConfig",
        "io.tolgee.model.automations.Automation",
        "io.tolgee.model.automations.AutomationTrigger",
        "io.tolgee.model.automations.AutomationAction",
        "io.tolgee.model.webhook.WebhookConfig",
        "io.tolgee.model.slackIntegration.SlackConfig",
        "io.tolgee.model.slackIntegration.SlackConfigPreference",
        "io.tolgee.model.slackIntegration.SavedSlackMessage",
        "io.tolgee.model.slackIntegration.SlackMessageInfo",
        "io.tolgee.model.slackIntegration.SlackUserConnection",
        "io.tolgee.model.slackIntegration.OrganizationSlackWorkspace",
        "io.tolgee.model.glossary.Glossary",
        "io.tolgee.model.glossary.GlossaryTerm",
        "io.tolgee.model.glossary.GlossaryTermTranslation",
        "io.tolgee.model.translationMemory.TranslationMemory",
        "io.tolgee.model.translationMemory.TranslationMemoryEntry",
        "io.tolgee.model.translationMemory.TranslationMemoryProject",
        "io.tolgee.model.notifications.Notification",
        "io.tolgee.model.notifications.NotificationSetting",
        "io.tolgee.model.UploadedImage",
        "io.tolgee.model.translationAgency.TranslationAgency",
        "io.tolgee.model.InstanceId",
        "io.tolgee.model.ForcedServerDateTime",
        "io.tolgee.ee.model.EeSubscription",
        "io.tolgee.ee.model.UsageToReport",
      )
    }

  private fun MutableMap<String, ExportImportPolicy>.owned(vararg classes: KClass<*>) =
    classes.forEach { classify(it, ExportImportPolicy.OWNED) }

  private fun MutableMap<String, ExportImportPolicy>.ignored(vararg classNames: String) =
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
