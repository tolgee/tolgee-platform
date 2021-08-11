package io.tolgee.configuration

interface Caches {
  companion object {
    const val USER_ACCOUNTS = "userAccounts"
    const val PROJECTS = "projects"
    const val PROJECT_PERMISSIONS = "projectPermissions"

    val caches = listOf(USER_ACCOUNTS, PROJECTS, PROJECT_PERMISSIONS)
  }
}
