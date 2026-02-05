package io.tolgee.unit.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.component.cache.LocalSchemaRevisionStore
import io.tolgee.configuration.tolgee.FileStorageProperties
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LocalSchemaRevisionStoreTest {
  @TempDir
  lateinit var tempDir: File

  private lateinit var store: LocalSchemaRevisionStore
  private val objectMapper = jacksonObjectMapper()

  @BeforeEach
  fun setUp() {
    val properties = TolgeeProperties()
    properties.fileStorage =
      FileStorageProperties().apply {
        fsDataPath = tempDir.absolutePath
      }
    store = LocalSchemaRevisionStore(properties, objectMapper)
  }

  @Test
  fun `getStoredRevisions returns empty map when file does not exist`() {
    val revisions = store.getStoredRevisions()

    assertThat(revisions).isEmpty()
  }

  @Test
  fun `storeRevisions creates file and getStoredRevisions reads it`() {
    val revisions = mapOf("cache1" to 1, "cache2" to 2)

    store.storeRevisions(revisions)
    val retrieved = store.getStoredRevisions()

    assertThat(retrieved).isEqualTo(revisions)
  }

  @Test
  fun `storeRevisions overwrites existing file`() {
    val initial = mapOf("cache1" to 1)
    val updated = mapOf("cache1" to 2, "cache2" to 1)

    store.storeRevisions(initial)
    store.storeRevisions(updated)
    val retrieved = store.getStoredRevisions()

    assertThat(retrieved).isEqualTo(updated)
  }

  @Test
  fun `getStoredRevisions returns empty map for empty file`() {
    val file = File(tempDir, ".cache_schema_revisions")
    file.writeText("")

    val revisions = store.getStoredRevisions()

    assertThat(revisions).isEmpty()
  }

  @Test
  fun `getStoredRevisions returns empty map for invalid JSON`() {
    val file = File(tempDir, ".cache_schema_revisions")
    file.writeText("not valid json")

    val revisions = store.getStoredRevisions()

    assertThat(revisions).isEmpty()
  }
}
