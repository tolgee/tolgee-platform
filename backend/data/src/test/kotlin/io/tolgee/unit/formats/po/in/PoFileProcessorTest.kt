package io.tolgee.unit.formats.po.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File
import java.io.InputStream

class PoFileProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext
  private lateinit var file: File

  @Test
  fun `processes standard file correctly`() {
    mockImportFile("example.po")
    PoFileProcessor(fileProcessorContext).process()
    assertThat(fileProcessorContext.languages).hasSize(1)
    assertThat(fileProcessorContext.translations).hasSize(8)
    val text = fileProcessorContext.translations["%d pages read."]?.get(0)?.text
    assertThat(text)
      .isEqualTo(
        "{0, plural,\n" +
          "one {Eine Seite gelesen wurde.}\n" +
          "other {{0, number} Seiten gelesen wurden.}\n" +
          "}",
      )
    assertThat(fileProcessorContext.translations.values.toList()[2][0].text)
      .isEqualTo("Willkommen zurück, {0}! Dein letzter Besuch war am {1}")
  }

  @Test
  fun `adds metadata`() {
    mockImportFile("example.po")
    PoFileProcessor(fileProcessorContext).process()
    val keyMeta =
      fileProcessorContext.keys[
        "We connect developers and translators around the globe " +
          "in Tolgee for a fantastic localization experience.",
      ]!!.keyMeta!!
    assertThat(keyMeta.description).isEqualTo(
      "This is the text that should appear next to menu accelerators * " +
        "that use the super key. If the text on this key isn't typically * " +
        "translated on keyboards used for your language, don't translate * this.",
    )
    assertThat(keyMeta.codeReferences).hasSize(6)
    assertThat(keyMeta.codeReferences[0].path).isEqualTo("light_interface.c")
    assertThat(keyMeta.codeReferences[0].line).isEqualTo(196)
  }

  @Test
  fun `processes windows newlines`() {
    val string = jacksonObjectMapper().readValue<String>(File("src/test/resources/import/po/windows-newlines.po.json"))
    assertThat(string).contains("\r\n")

    mockImportFile(string.byteInputStream())
    PoFileProcessor(fileProcessorContext).process()
    assertThat(fileProcessorContext.languages).hasSize(1)
    assertThat(fileProcessorContext.translations).hasSize(1)
    assertThat(fileProcessorContext.translations.values.toList()[0][0].text)
      .isEqualTo("# Hex код (#fff)")
  }

  private fun mockImportFile(fileName: String) {
    file = File("src/test/resources/import/po/$fileName")
    mockImportFile(file.inputStream())
  }

  private fun mockImportFile(inputStream: InputStream) {
    importMock = mock()
    importFile = ImportFile("exmample.po", importMock)
    importFileDto =
      ImportFileDto(
        "exmample.po",
        inputStream.readAllBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }
}
