package io.tolgee.unit.formats.csv.`in`

import io.tolgee.formats.csv.`in`.CSVImportFormatDetector
import io.tolgee.formats.csv.`in`.CsvFileParser
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class CsvImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected i18next`() {
    "src/test/resources/import/csv/example.csv".assertDetected(ImportFormat.CSV_ICU)
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/csv/icu.csv".assertDetected(ImportFormat.CSV_ICU)
  }

  @Test
  fun `detected java`() {
    "src/test/resources/import/csv/java.csv".assertDetected(ImportFormat.CSV_JAVA)
  }

  @Test
  fun `detected php`() {
    "src/test/resources/import/csv/php.csv".assertDetected(ImportFormat.CSV_PHP)
  }

  @Test
  fun `fallbacks to icu`() {
    "src/test/resources/import/csv/unknown.csv".assertDetected(ImportFormat.CSV_ICU)
  }

  private fun parseFile(path: String): Any? {
    val parser =
      CsvFileParser(
        inputStream = File(path).inputStream(),
        delimiter = ',',
        languageFallback = "unknown",
      )
    return parser.rows
  }

  private fun String.assertDetected(format: ImportFormat) {
    CSVImportFormatDetector().detectFormat(parseFile(this)).assert.isEqualTo(format)
  }
}
