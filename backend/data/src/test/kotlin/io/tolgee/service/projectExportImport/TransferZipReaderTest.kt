package io.tolgee.service.projectExportImport

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.service.projectExportImport.model.ExportManifest
import io.tolgee.service.projectExportImport.model.ExportZipLayout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class TransferZipReaderTest {
  private val objectMapper = jacksonObjectMapper()
  private val reader = TransferZipReader(objectMapper)

  @Test
  fun `reads manifest, project, entities and blobs`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        ExportZipLayout.PROJECT to """{"handle":1,"attrs":{"name":"src"},"assocs":{}}""".toByteArray(),
        ExportZipLayout.entityPath("Key") to "[]".toByteArray(),
        ExportZipLayout.blobPath("screenshots/9.jpg") to byteArrayOf(1, 2, 3),
      )

    val parsed = reader.read(zip.inputStream())

    assertThat(parsed.manifest.sourceProjectName).isEqualTo("src")
    assertThat(parsed.projectJson).isNotNull()
    assertThat(parsed.entityJsonByType).containsKey("Key")
    assertThat(parsed.blobs).containsKey("screenshots/9.jpg")
    assertThat(parsed.blobs.getValue("screenshots/9.jpg")).containsExactly(1, 2, 3)
  }

  @Test
  fun `routes a sidechannels entry into sideChannels by its path prefix (not the manifest)`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        ExportZipLayout.BIG_META to "[]".toByteArray(),
        "${ExportZipLayout.SIDE_CHANNELS_DIR}future.json" to byteArrayOf(7),
      )

    val parsed = reader.read(zip.inputStream())

    assertThat(
      parsed.sideChannels,
    ).containsKeys(ExportZipLayout.BIG_META, "${ExportZipLayout.SIDE_CHANNELS_DIR}future.json")
    assertThat(parsed.sideChannels.getValue(ExportZipLayout.BIG_META)).isEqualTo("[]".toByteArray())
  }

  @Test
  fun `rejects an absolute zip entry path (zip-slip)`() {
    val zip = buildZip("/etc/passwd" to byteArrayOf(1))
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("absolute")
  }

  @Test
  fun `rejects a parent-escaping zip entry path (zip-slip)`() {
    val zip = buildZip("../../evil.json" to byteArrayOf(1))
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("escapes")
  }

  @Test
  fun `rejects an entry over the per-entry uncompressed cap (zip-bomb)`() {
    val cappedReader = TransferZipReader(objectMapper, maxEntryUncompressedBytes = 1024)
    val zip = buildZip(ExportZipLayout.entityPath("Key") to ByteArray(2048) { 0 })
    assertThatThrownBy { cappedReader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("per-entry uncompressed cap")
  }

  @Test
  fun `rejects an archive over the total uncompressed cap (zip-bomb)`() {
    val cappedReader = TransferZipReader(objectMapper, maxTotalUncompressedBytes = 1024)
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        ExportZipLayout.entityPath("Key") to ByteArray(2048) { 0 },
      )
    assertThatThrownBy { cappedReader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("total uncompressed cap")
  }

  @Test
  fun `rejects an archive over the entry-count cap`() {
    val cappedReader = TransferZipReader(objectMapper, maxEntries = 1)
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        ExportZipLayout.entityPath("Key") to "[]".toByteArray(),
      )
    assertThatThrownBy { cappedReader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("too many entries")
  }

  @Test
  fun `rejects an entry outside the known layout`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        "stowaway.txt" to byteArrayOf(1),
      )
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("unexpected entry")
  }

  @Test
  fun `rejects an entry name with a control character`() {
    val zip = buildZip("entities/Key\u0001.json" to byteArrayOf(1))
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("invalid entry name")
  }

  @Test
  fun `rejects an entity entry without a json suffix`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        "${ExportZipLayout.ENTITIES_DIR}Key" to "[]".toByteArray(),
      )
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("invalid entity entry")
  }

  @Test
  fun `rejects an entity entry with an empty type name`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        "${ExportZipLayout.ENTITIES_DIR}.json" to "[]".toByteArray(),
      )
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("invalid entity entry")
  }

  @Test
  fun `rejects a nested entity entry path`() {
    val zip =
      buildZip(
        ExportZipLayout.MANIFEST to objectMapper.writeValueAsBytes(manifest()),
        "${ExportZipLayout.ENTITIES_DIR}sub/Key.json" to "[]".toByteArray(),
      )
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("invalid entity entry")
  }

  @Test
  fun `fails when the manifest is missing`() {
    val zip = buildZip(ExportZipLayout.entityPath("Key") to "[]".toByteArray())
    assertThatThrownBy { reader.read(zip.inputStream()) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining("manifest")
  }

  private fun manifest() =
    ExportManifest(
      schemaVersion = "1.0.0",
      sourceProjectName = "src",
      exportedAt = 0,
      entityCounts = emptyMap(),
    )

  private fun buildZip(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
      entries.forEach { (name, bytes) ->
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
      }
    }
    return out.toByteArray()
  }
}
