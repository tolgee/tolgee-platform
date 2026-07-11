package io.tolgee.unit.xlsx.`in`

import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.xlsx.`in`.XlsxFileParser
import io.tolgee.formats.xlsx.`in`.XlsxImportFormatDetector
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class XlsxImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected i18next`() {
    "src/test/resources/import/xlsx/example.xlsx".assertDetected(ImportFormat.XLSX_ICU)
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/xlsx/icu.xlsx".assertDetected(ImportFormat.XLSX_ICU)
  }

  @Test
  fun `detected java`() {
    "src/test/resources/import/xlsx/java.xlsx".assertDetected(ImportFormat.XLSX_JAVA)
  }

  @Test
  fun `detected php`() {
    "src/test/resources/import/xlsx/php.xlsx".assertDetected(ImportFormat.XLSX_PHP)
  }

  @Test
  fun `fallbacks to icu`() {
    "src/test/resources/import/xlsx/unknown.xlsx".assertDetected(ImportFormat.XLSX_ICU)
  }

  private fun parseFile(path: String): Any {
    val parser =
      XlsxFileParser(
        inputStream = File(path).inputStream(),
        languageFallback = "unknown",
      )
    return parser.rawData
  }

  private fun String.assertDetected(format: ImportFormat) {
    XlsxImportFormatDetector().detectFormat(parseFile(this)).assert.isEqualTo(format)
  }
}
