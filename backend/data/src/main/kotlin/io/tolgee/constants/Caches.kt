package io.tolgee.constants

interface Caches {
  companion object {
    const val BUSINESS_EVENT_THROTTLING = "businessEventThrottling"
    const val USER_ACCOUNTS = "userAccounts"
    const val PROJECTS = "projects"
    const val PERMISSIONS = "permissions"
    const val BATCH_JOBS = "batchJobs"
    const val RATE_LIMITS = "rateLimits"
    const val MACHINE_TRANSLATIONS = "machineTranslations"
    const val PROJECT_TRANSLATIONS_MODIFIED = "projectTranslationsModified"
    const val USAGE = "usage"
    const val DISMISSED_ANNOUNCEMENT = "dismissedAnnouncement"

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
