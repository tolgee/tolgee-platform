package io.tolgee.unit.formats.ios.`in`

import StringsdictFileProcessor
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class StringsdictFormatProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("example.stringsdict", importMock)
    importFileDto =
      ImportFileDto(
        "example.stringsdict",
        File("src/test/resources/import/ios/example.stringsdict")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    StringsdictFileProcessor(fileProcessorContext).process()
    Assertions.assertThat(fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(fileProcessorContext.translations).hasSize(2)
    fileProcessorContext.translations["what-a-key-plural"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Peter has # dog}\n" +
        "other {Peter hase # dogs}\n" +
        "}",
    )
    fileProcessorContext.translations["what-a-key-plural-2"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Lucy has # '{dog}'}\n" +
        "other {Lucy has # '{dogs}'}\n" +
        "}",
    )
  }
}
