package io.tolgee.service.projectExportImport

import io.tolgee.service.projectExportImport.model.ExportManifest
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Paths
import java.util.zip.ZipInputStream

/**
 * Reads an export zip ([ExportZipLayout]) into memory, hardened against hostile archives:
 *
 * - **Zip-slip:** every entry name must be relative and stay inside the archive root after normalization
 *   (`PathSecurity.sanitizePath` strips leading `..` but lets absolute paths through, so containment is
 *   checked here). The result is keyed by the validated name; entries are never written to disk by name.
 * - **Zip-bomb:** each entry and the whole archive are decompressed under hard uncompressed-size and
 *   entry-count caps, so a small zip can't expand into an OOM.
 */
@Component
class TransferZipReader(
  private val objectMapper: ObjectMapper,
  private val maxEntryUncompressedBytes: Long = MAX_ENTRY_UNCOMPRESSED_BYTES,
  private val maxTotalUncompressedBytes: Long = MAX_TOTAL_UNCOMPRESSED_BYTES,
  private val maxEntries: Int = MAX_ENTRIES,
) {
  fun read(input: InputStream): ParsedExport {
    val entries = LinkedHashMap<String, ByteArray>()
    var totalBytes = 0L
    ZipInputStream(input).use { zip ->
      generateSequence { zip.nextEntry }.forEach { entry ->
        if (entry.isDirectory) return@forEach
        val name = safeEntryName(entry.name)
        require(entries.size < maxEntries) { "Export zip has too many entries (> $maxEntries)" }
        val bytes = readCapped(zip, name)
        totalBytes += bytes.size
        require(totalBytes <= maxTotalUncompressedBytes) {
          "Export zip exceeds the $maxTotalUncompressedBytes-byte total uncompressed cap"
        }
        require(entries.put(name, bytes) == null) { "Export zip has a duplicate entry: $name" }
      }
    }

    val manifestBytes = requireNotNull(entries[ExportZipLayout.MANIFEST]) { "Export zip has no manifest" }
    val manifest = objectMapper.readValue(manifestBytes, ExportManifest::class.java)

    val entityJsonByType = LinkedHashMap<String, ByteArray>()
    val sideChannels = LinkedHashMap<String, ByteArray>()
    val blobs = LinkedHashMap<String, ByteArray>()
    entries.forEach { (name, bytes) ->
      when {
        name == ExportZipLayout.MANIFEST || name == ExportZipLayout.PROJECT -> {}
        // Routed by directory prefix like entities/ and blobs/ (not by manifest content), so a hostile
        // manifest can't reclassify a real entry.
        name.startsWith(ExportZipLayout.SIDE_CHANNELS_DIR) -> sideChannels[name] = bytes
        name.startsWith(ExportZipLayout.ENTITIES_DIR) ->
          entityJsonByType[entityTypeOf(name)] = bytes
        name.startsWith(ExportZipLayout.BLOBS_DIR) ->
          blobs[name.removePrefix(ExportZipLayout.BLOBS_DIR)] = bytes
        else -> throw IllegalArgumentException("Export zip has an unexpected entry: $name")
      }
    }
    return ParsedExport(
      manifest,
      entries[ExportZipLayout.PROJECT],
      sideChannels,
      entityJsonByType,
      blobs,
    )
  }

  private fun safeEntryName(rawName: String): String {
    require(rawName.isNotBlank() && rawName.none { it.isISOControl() }) { "Export zip has an invalid entry name" }
    val path = Paths.get(rawName)
    require(!path.isAbsolute) { "Export zip has an absolute entry path: $rawName" }
    val normalized = path.normalize()
    require(!normalized.startsWith("..") && normalized.toString().isNotEmpty()) {
      "Export zip entry escapes the archive root: $rawName"
    }
    return normalized.toString()
  }

  private fun readCapped(
    zip: ZipInputStream,
    name: String,
  ): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    var size = 0L
    while (true) {
      val read = zip.read(buffer)
      if (read < 0) break
      size += read
      require(size <= maxEntryUncompressedBytes) {
        "Export zip entry '$name' exceeds the $maxEntryUncompressedBytes-byte per-entry uncompressed cap"
      }
      out.write(buffer, 0, read)
    }
    return out.toByteArray()
  }

  private fun entityTypeOf(name: String): String {
    val relative = name.removePrefix(ExportZipLayout.ENTITIES_DIR)
    val entityType = relative.removeSuffix(".json")
    require(entityType != relative && entityType.isNotEmpty() && "/" !in entityType) {
      "Export zip has an invalid entity entry: $name"
    }
    return entityType
  }

  data class ParsedExport(
    val manifest: ExportManifest,
    val projectJson: ByteArray?,
    val sideChannels: Map<String, ByteArray>,
    val entityJsonByType: Map<String, ByteArray>,
    val blobs: Map<String, ByteArray>,
  )

  companion object {
    const val MAX_ENTRY_UNCOMPRESSED_BYTES = 500L * 1024 * 1024
    const val MAX_TOTAL_UNCOMPRESSED_BYTES = 2L * 1024 * 1024 * 1024
    const val MAX_ENTRIES = 100_000
  }
}
