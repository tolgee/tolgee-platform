package io.tolgee.service.apps

/**
 * Refuses app installs/registrations beyond what the edition allows. The default counts
 * server-wide registered apps; the cloud billing module replaces it with per-organization
 * install counting against the organization's plan.
 */
interface AppsLimitGuard {
  /**
   * Called before an install is created. [registersNewApp] is true when the call also registers
   * the app on this server (as opposed to installing an already-registered one).
   */
  fun checkAppsLimit(
    organizationId: Long,
    registersNewApp: Boolean,
  )
}
