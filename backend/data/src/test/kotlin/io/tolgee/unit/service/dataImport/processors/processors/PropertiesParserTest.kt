package io.tolgee.unit.service.dataImport.processors.processors

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.PropertyFileProcessor
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class PropertiesParserTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext


  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("messages_en.properties", importMock)
    importFileDto = ImportFileDto(
      "messages_en.properties",
      File("src/test/resources/import/example.properties")
        .inputStream()
    )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    PropertyFileProcessor(fileProcessorContext).process()
    Assertions.assertThat(fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(fileProcessorContext.translations).hasSize(4)
    val text = fileProcessorContext.translations["Register"]?.get(0)?.text
    Assertions.assertThat(text).isEqualTo("Veuillez vous enregistrer sur la page suivante.")
    val multiLineText = fileProcessorContext.translations["Cleanup"]?.get(0)?.text
    Assertions.assertThat(multiLineText).hasLineCount(3)
  }

}
