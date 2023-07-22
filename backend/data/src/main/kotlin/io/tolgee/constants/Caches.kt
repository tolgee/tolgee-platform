package io.tolgee.constants

interface Caches {
  companion object {
    const val BUSINESS_EVENT_THROTTLING = "businessEventThrottling"
    const val USER_ACCOUNTS = "userAccounts"
    const val PROJECTS = "projects"
    const val PERMISSIONS = "permissions"
    const val RATE_LIMITS = "rateLimits"
    const val MACHINE_TRANSLATIONS = "machineTranslations"
    const val PROJECT_TRANSLATIONS_MODIFIED = "projectTranslationsModified"
    const val USAGE = "usage"

    val caches = listOf(
      USER_ACCOUNTS,
      PROJECTS,
      PERMISSIONS,
      MACHINE_TRANSLATIONS,
      PROJECT_TRANSLATIONS_MODIFIED,
      BUSINESS_EVENT_THROTTLING,
      USAGE
    )
  }
}
