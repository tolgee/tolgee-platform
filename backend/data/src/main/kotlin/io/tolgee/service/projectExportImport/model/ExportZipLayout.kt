package io.tolgee.service.projectExportImport.model

/**
 * The on-disk layout of an export zip:
 * ```
 * manifest.json                 # schemaVersion, sourceProjectName, exportedAt, entityCounts, sideChannelCounts
 * project.json                  # the PROJECT_ROOT row's own scalar columns (mirrored on import)
 * sidechannels/<name>.json      # one SIDE_CHANNEL type per file (e.g. bigMeta.json), remapped on import
 * entities/<EntityType>.json    # one JSON array of SerializedEntity per OWNED type (simple name)
 * blobs/<name>                  # screenshot images + project avatar, named by source handle
 * ```
 */
object ExportZipLayout {
  const val MANIFEST = "manifest.json"
  const val PROJECT = "project.json"
  const val SIDE_CHANNELS_DIR = "sidechannels/"
  const val BIG_META = SIDE_CHANNELS_DIR + "bigMeta.json"
  const val ENTITIES_DIR = "entities/"
  const val BLOBS_DIR = "blobs/"

  fun entityPath(entityTypeSimpleName: String): String = "$ENTITIES_DIR$entityTypeSimpleName.json"

  fun blobPath(blobName: String): String = "$BLOBS_DIR$blobName"
}
