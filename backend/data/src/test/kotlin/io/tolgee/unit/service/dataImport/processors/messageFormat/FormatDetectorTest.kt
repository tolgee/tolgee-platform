package io.tolgee.unit.service.dataImport.processors.messageFormat

import io.tolgee.testing.assertions.Assertions.assertThat
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.messageFormat.FormatDetector
import io.tolgee.service.dataImport.processors.messageFormat.SupportedFormat
import org.mockito.kotlin.mock
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class FormatDetectorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeMethod
  fun setup() {
    importMock = mock()
    importFile = ImportFile("exmample.po", importMock)
    importFileDto = ImportFileDto(
      "exmample.po",
      File("src/test/resources/import/po/example.po")
        .inputStream()
    )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile, mock())
  }

  @Test
  fun `returns C format`() {
    val detector = FormatDetector(listOf("%jd %hhd", "%d %s", "d %s"))
    assertThat(detector()).isEqualTo(SupportedFormat.C)
  }

  @Test
  fun `returns PHP format`() {
    val detector = FormatDetector(listOf("%b %d", "%d %s", "d %s"))
    assertThat(detector()).isEqualTo(SupportedFormat.PHP)
  }
}
