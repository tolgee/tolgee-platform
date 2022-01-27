package io.tolgee.constants

interface Caches {
  companion object {
    const val USER_ACCOUNTS = "userAccounts"
    const val PROJECTS = "projects"
    const val PROJECT_PERMISSIONS = "projectPermissions"
    const val RATE_LIMITS = "rateLimits"
    const val MACHINE_TRANSLATIONS = "machineTranslations"

    val caches = listOf(USER_ACCOUNTS, PROJECTS, PROJECT_PERMISSIONS, MACHINE_TRANSLATIONS)
  }
}
