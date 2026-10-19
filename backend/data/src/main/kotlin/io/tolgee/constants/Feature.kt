@file:Suppress("unused")

package io.tolgee.constants

// Persisted by name in several tables (in billing too); removing a constant needs a data
// migration deleting those rows, or loading them fails.
enum class Feature(
  // In development: invisible to users unless granted to their organization.
  // Mirrored in webapp useFeatures.tsx (HiddenFeature).
  val hidden: Boolean = false,
) {
  GRANULAR_PERMISSIONS,
  PRIORITIZED_FEATURE_REQUESTS,
  PREMIUM_SUPPORT,
  DEDICATED_SLACK_CHANNEL,
  ASSISTED_UPDATES,
  DEPLOYMENT_ASSISTANCE,
  BACKUP_CONFIGURATION,
  TEAM_TRAINING,
  ACCOUNT_MANAGER,
  STANDARD_SUPPORT,
  PROJECT_LEVEL_CONTENT_STORAGES,
  WEBHOOKS,
  MULTIPLE_CONTENT_DELIVERY_CONFIGS,
  AI_PROMPT_CUSTOMIZATION,
  SLACK_INTEGRATION,
  TASKS,
  SSO,
  ORDER_TRANSLATION,
  GLOSSARY,
  TRANSLATION_LABELS,
  BRANCHING,
  QA_CHECKS,
  TRANSLATION_MEMORY,
  AI_REVIEW(hidden = true),
  ;

  companion object {
    fun findByName(name: String): Feature? {
      return entries.find { it.name == name }
    }
  }
}
