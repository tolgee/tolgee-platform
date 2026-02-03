package io.tolgee.unit.formats.properties.`in`

import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.properties.`in`.PropertiesImportFormatDetector
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.io.FileHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PropertiesImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/properties/icu.properties".assertDetected(ImportFormat.PROPERTIES_ICU)
  }

  @Test
  fun `detected icu with plural only`() {
    "src/test/resources/import/properties/icu_plural.properties".assertDetected(ImportFormat.PROPERTIES_ICU)
  }

  @Test
  fun `detected jav`() {
    "src/test/resources/import/properties/java.properties".assertDetected(ImportFormat.PROPERTIES_JAVA)
  }

  private fun parseFile(path: String): Map<*, *> {
    val config = PropertiesConfiguration()
    val handler = FileHandler(config)
    handler.load(File(path).inputStream())
    return config.keys
      .asSequence()
      .map { key -> key to config.getString(key) }
      .toMap()
  }

  private fun String.assertDetected(format: ImportFormat) {
    val parsed = parseFile(this)
    val detected = PropertiesImportFormatDetector().detectFormat(parsed)
    detected.assert.isEqualTo(format)
  }
}
