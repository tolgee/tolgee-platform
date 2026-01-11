package io.tolgee.unit.formats.xliff.`in`

import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.xliff.`in`.XliffImportFormatDetector
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.formats.xliff.model.XliffModel
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class XliffImportFormatDetectorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `detected ruby`() {
    "src/test/resources/import/xliff/ruby.xliff".assertDetected(ImportFormat.XLIFF_RUBY)
  }

  @Test
  fun `detected icu`() {
    "src/test/resources/import/xliff/icu.xliff".assertDetected(ImportFormat.XLIFF_ICU)
  }

  @Test
  fun `detected java`() {
    "src/test/resources/import/xliff/java.xliff".assertDetected(ImportFormat.XLIFF_JAVA)
  }

  @Test
  fun `detected php`() {
    "src/test/resources/import/xliff/php.xliff".assertDetected(ImportFormat.XLIFF_PHP)
  }

  private fun parseFile(path: String): XliffModel {
    val xmlEventReader = getXmlEventReader(File(path).inputStream())
    return XliffParser(xmlEventReader).parse()
  }

  private fun getXmlEventReader(inputStream: InputStream): XMLEventReader {
    val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
    return inputFactory.createXMLEventReader(inputStream)
  }

  private fun String.assertDetected(format: ImportFormat) {
    val parsed = parseFile(this)
    val detected = XliffImportFormatDetector().detectFormat(parsed)
    detected.assert.isEqualTo(format)
  }
}
