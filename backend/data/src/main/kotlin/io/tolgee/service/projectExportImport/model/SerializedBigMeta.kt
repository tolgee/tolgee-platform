package io.tolgee.service.projectExportImport.model

/** One `KeysDistance` (BigMeta) row in `bigMeta.json`, carrying source key ids remapped on import. */
data class SerializedBigMeta(
  val key1Id: Long = 0,
  val key2Id: Long = 0,
  val distance: Double = 0.0,
  val hits: Long = 1,
)
