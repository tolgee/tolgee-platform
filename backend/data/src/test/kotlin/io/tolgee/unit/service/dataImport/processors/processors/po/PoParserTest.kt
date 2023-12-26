package io.tolgee.unit.service.dataImport.processors.processors.po

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.po.PoParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class PoParserTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("exmample.po", importMock)
    importFileDto =
      ImportFileDto(
        "exmample.po",
        File("src/test/resources/import/po/example.po")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    val result = PoParser(fileProcessorContext)()
    assertThat(result.translations).hasSizeGreaterThan(8)
    assertThat(result.translations[5].msgstrPlurals).hasSize(2)
    assertThat(result.translations[5].msgstrPlurals!![0].toString()).isEqualTo("Eine Seite gelesen wurde.")
    assertThat(result.translations[5].msgstrPlurals!![1].toString()).isEqualTo("%d Seiten gelesen wurden.")
    assertThat(result.translations[2].meta.translatorComments).hasSize(2)
    assertThat(result.translations[2].meta.translatorComments[0]).isEqualTo("some other comment")
    assertThat(result.translations[2].meta.extractedComments).hasSize(4)
  }
}
