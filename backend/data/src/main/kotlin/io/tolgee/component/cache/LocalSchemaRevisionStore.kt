package io.tolgee.component.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.util.Logging
import io.tolgee.util.logger
import org.springframework.stereotype.Component
import java.io.File

/**
 * Local file-based implementation of [SchemaRevisionStore].
 *
 * Stores schema revisions as JSON in a file within Tolgee's data directory.
 * This is used as a fallback when Redis is not available.
 *
 * The file is stored at: `{tolgee.file-storage.fs-data-path}/.cache_schema_revisions`
 */
@Component
class LocalSchemaRevisionStore(
  private val tolgeeProperties: TolgeeProperties,
  private val objectMapper: ObjectMapper,
) : SchemaRevisionStore,
  Logging {
  companion object {
    private const val REVISIONS_FILE_NAME = ".cache_schema_revisions"
  }

  private val revisionsFile: File
    get() = File(tolgeeProperties.fileStorage.fsDataPath, REVISIONS_FILE_NAME)

  override fun getStoredRevisions(): Map<String, Int> {
    return try {
      if (revisionsFile.exists()) {
        val json = revisionsFile.readText().trim()
        if (json.isNotEmpty()) {
          objectMapper.readValue(json)
        } else {
          emptyMap()
        }
      } else {
        emptyMap()
      }
    } catch (e: Exception) {
      logger.warn("Failed to read schema revisions from file", e)
      emptyMap()
    }
  }

  override fun storeRevisions(revisions: Map<String, Int>) {
    try {
      revisionsFile.parentFile?.mkdirs()
      val json = objectMapper.writeValueAsString(revisions)
      revisionsFile.writeText(json)
    } catch (e: Exception) {
      logger.warn("Failed to write schema revisions to file", e)
    }
  }
}
