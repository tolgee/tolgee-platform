package io.tolgee.unit.service.dataImport.processors.processors.po

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.service.dataImport.processors.po.PoFileProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class PoFileProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
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
  fun `processes po file correctly`() {
    PoFileProcessor(fileProcessorContext).process()
    assertThat(fileProcessorContext.languages).hasSize(1)
    assertThat(fileProcessorContext.translations).hasSize(8)
    assertThat(fileProcessorContext.translations["%d pages read."]?.get(0)?.text)
      .isEqualTo(
        "{0, plural,\n" +
          "one {Eine Seite gelesen wurde.}\n" +
          "other {{0, number} Seiten gelesen wurden.}\n" +
          "}"
      )
    assertThat(fileProcessorContext.translations.values.toList()[2][0].text)
      .isEqualTo("Willkommen zurück, {0}! Dein letzter Besuch war am {1}")
  }

  @Test
  fun `adds metadata`() {
    PoFileProcessor(fileProcessorContext).process()
    val keyMeta = fileProcessorContext.keys[
      "We connect developers and translators around the globe " +
        "in Tolgee for a fantastic localization experience."
    ]!!.keyMeta!!
    assertThat(keyMeta.comments).hasSize(2)
    assertThat(keyMeta.comments[0].text).isEqualTo(
      "This is the text that should appear next to menu accelerators" +
        " * that use the super key. If the text on this key isn't typically" +
        " * translated on keyboards used for your language, don't translate * this."
    )
    assertThat(keyMeta.comments[1].text).isEqualTo("some other comment and other")
    assertThat(keyMeta.codeReferences).hasSize(6)
    assertThat(keyMeta.codeReferences[0].path).isEqualTo("light_interface.c")
    assertThat(keyMeta.codeReferences[0].line).isEqualTo(196)
  }
}
