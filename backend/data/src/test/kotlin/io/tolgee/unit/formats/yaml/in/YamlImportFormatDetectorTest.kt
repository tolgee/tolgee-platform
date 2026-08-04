package io.tolgee.unit.formats.yaml.`in`

import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.yaml.`in`.YamlImportFormatDetector
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.module.kotlin.readValue
import java.io.File

class YamlImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected ruby`() {
    "src/test/resources/import/yaml/ruby.yaml".assertDetected(ImportFormat.YAML_RUBY)
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/yaml/icu.yaml".assertDetected(ImportFormat.YAML_ICU)
  }

  @Test
  fun `detected java`() {
    "src/test/resources/import/yaml/java.yaml".assertDetected(ImportFormat.YAML_JAVA)
  }

  @Test
  fun `fallbacks to unknown`() {
    "src/test/resources/import/yaml/unknown.yaml".assertDetected(ImportFormat.YAML_UNKNOWN)
  }

  private fun parseFile(path: String): Map<*, *> {
    return ObjectMapper(YAMLFactory()).readValue<Map<*, *>>(
      File(path)
        .readBytes(),
    )
  }

  private fun String.assertDetected(format: ImportFormat) {
    val parsed = parseFile(this)
    val detected = YamlImportFormatDetector().detectFormat(parsed)
    detected.assert.isEqualTo(format)
  }
}
