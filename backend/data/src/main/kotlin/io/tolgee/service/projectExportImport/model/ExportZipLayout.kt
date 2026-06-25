package io.tolgee.service.projectExportImport.model

/**
 * The on-disk layout of an export zip:
 * ```
 * manifest.json                 # schemaVersion, sourceProjectName, exportedAt, entityCounts
 * project.json                  # the PROJECT_ROOT row's own scalar columns (mirrored on import)
 * entities/<EntityType>.json    # one JSON array of SerializedEntity per OWNED type (simple name)
 * blobs/<name>                  # screenshot images + project avatar, named by source handle
 * ```
 */
object ExportZipLayout {
  const val MANIFEST = "manifest.json"
  const val PROJECT = "project.json"
  const val ENTITIES_DIR = "entities/"
  const val BLOBS_DIR = "blobs/"

  fun entityPath(entityTypeSimpleName: String): String = "$ENTITIES_DIR$entityTypeSimpleName.json"

  fun blobPath(blobName: String): String = "$BLOBS_DIR$blobName"
}
