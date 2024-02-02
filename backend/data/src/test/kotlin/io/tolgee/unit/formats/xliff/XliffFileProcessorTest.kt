package io.tolgee.unit.formats.xliff

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.xliff.`in`.XliffFileProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class XliffFileProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("exmample.xliff", importMock)
    importFileDto =
      ImportFileDto(
        "exmample.xliff",
        File("src/test/resources/import/xliff/example.xliff")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `processes xliff 12 file`() {
    XliffFileProcessor(fileProcessorContext).process()
    assertThat(fileProcessorContext.languages).hasSize(2)
    assertThat(fileProcessorContext.translations).hasSize(176)
  }
}
