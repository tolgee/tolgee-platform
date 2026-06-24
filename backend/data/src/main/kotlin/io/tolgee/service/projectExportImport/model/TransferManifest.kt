package io.tolgee.service.projectExportImport.model

/**
 * `manifest.json` at the root of an export zip. The import refuses a [schemaVersion] that does not
 * equal the running Tolgee version — the export format is the live entity shape, so there is no
 * cross-version compatibility (stated on the UI).
 */
data class TransferManifest(
  val schemaVersion: String,
  val sourceProjectName: String,
  val exportedAt: Long,
  val entityCounts: Map<String, Int>,
)
