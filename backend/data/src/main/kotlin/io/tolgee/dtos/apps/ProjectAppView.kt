package io.tolgee.dtos.apps

/**
 * One row of the project apps management listing, assembled by a single projection query so the
 * listing never fans out into a query per install. [enabledRowId] is the id of the enablement row
 * for this project, or null when the app is not enabled for it.
 */
data class ProjectAppView(
  val installId: Long,
  val appId: String,
  val name: String,
  val version: String,
  val baseUrl: String,
  val manifestJson: String,
  val enabledRowId: Long?,
) {
  val enabled: Boolean
    get() = enabledRowId != null
}
