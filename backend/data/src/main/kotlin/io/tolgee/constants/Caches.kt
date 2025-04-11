package io.tolgee.constants

interface Caches {
  companion object {
    const val BUSINESS_EVENT_THROTTLING = "businessEventThrottling"
    const val USER_ACCOUNTS = "userAccounts"
    const val PROJECT_API_KEYS = "projectApiKeys"
    const val PERSONAL_ACCESS_TOKENS = "personalAccessTokens"
    const val ORGANIZATIONS = "organizations"
    const val PROJECTS = "projects"
    const val PERMISSIONS = "permissions"
    const val BATCH_JOBS = "batchJobs"
    const val RATE_LIMITS = "rateLimits"
    const val MACHINE_TRANSLATIONS = "machineTranslations"
    const val PROJECT_TRANSLATIONS_MODIFIED = "projectTranslationsModified"
    const val USAGE = "usage"
    const val DISMISSED_ANNOUNCEMENT = "dismissedAnnouncement"
    const val AUTOMATIONS = "automations"
    const val EE_SUBSCRIPTION = "eeSubscription"
    const val LANGUAGES = "languages"
    const val ORGANIZATION_ROLES = "organizationRoles"
    const val SSO_TENANTS = "ssoTenants"
    const val LLM_PROVIDERS = "llmProviders"

    val caches =
      listOf(
        USER_ACCOUNTS,
        PROJECT_API_KEYS,
        PERSONAL_ACCESS_TOKENS,
        ORGANIZATIONS,
        PROJECTS,
        PERMISSIONS,
        MACHINE_TRANSLATIONS,
        PROJECT_TRANSLATIONS_MODIFIED,
        BUSINESS_EVENT_THROTTLING,
        USAGE,
        DISMISSED_ANNOUNCEMENT,
        AUTOMATIONS,
        EE_SUBSCRIPTION,
        LANGUAGES,
        ORGANIZATION_ROLES,
        SSO_TENANTS,
        LLM_PROVIDERS,
      )
  }
}
