package io.tolgee.unit.formats.json.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.json.`in`.JsonImportFormatDetector
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class JsonImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected i18next`() {
    "src/test/resources/import/i18next/example.json".assertDetected(ImportFormat.JSON_I18NEXT)
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/json/icu.json".assertDetected(ImportFormat.JSON_ICU)
  }

  @Test
  fun `detected java`() {
    "src/test/resources/import/json/java.json".assertDetected(ImportFormat.JSON_JAVA)
  }

  @Test
  fun `detected php`() {
    "src/test/resources/import/json/php.json".assertDetected(ImportFormat.JSON_PHP)
  }

  @Test
  fun `fallbacks to icu`() {
    "src/test/resources/import/json/unknown.json".assertDetected(ImportFormat.JSON_ICU)
  }

  private fun parseFile(path: String): Map<*, *> {
    return jacksonObjectMapper().readValue<Map<*, *>>(
      File(path)
        .readBytes(),
    )
  }

  private fun String.assertDetected(format: ImportFormat) {
    val parsed = parseFile(this)
    val detected = JsonImportFormatDetector().detectFormat(parsed)
    detected.assert.isEqualTo(format)
  }
}
