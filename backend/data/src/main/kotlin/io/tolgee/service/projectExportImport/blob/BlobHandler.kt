package io.tolgee.service.projectExportImport.blob

import kotlin.reflect.KClass

/**
 * Exports the bytes an entity owns outside the database (screenshot images, the project avatar).
 *
 * [BlobEntry.name] must be derived from the entity's source handle, never its storage path: the path is
 * PK/timestamp/hash-derived and differs on the target instance, so import can only locate a blob by handle.
 */
interface BlobHandler {
  val entityClass: KClass<*>

  fun export(entity: Any): List<BlobEntry>
}

data class BlobEntry(
  val name: String,
  val bytes: ByteArray,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is BlobEntry) return false
    return name == other.name && bytes.contentEquals(other.bytes)
  }

  override fun hashCode(): Int = 31 * name.hashCode() + bytes.contentHashCode()
}
