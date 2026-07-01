package io.tolgee.service.projectExportImport.model

/**
 * `manifest.json` at the root of an export zip. [schemaVersion] is the exact running Tolgee version —
 * the export format is the live entity shape, with no cross-version compatibility.
 */
data class ExportManifest(
  val schemaVersion: String,
  val sourceProjectName: String,
  val exportedAt: Long,
  val entityCounts: Map<String, Int>,
)
