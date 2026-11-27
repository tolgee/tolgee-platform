@file:Suppress("unused")

package io.tolgee.constants

// Stored by name in varchar[] columns, billing's included; removing a constant needs a migration
// that removes the name from those arrays (array_remove), or loading them fails.
enum class Feature(
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
