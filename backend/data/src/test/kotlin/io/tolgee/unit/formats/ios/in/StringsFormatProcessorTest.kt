package io.tolgee.unit.formats.ios.`in`

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.ios.`in`.strings.StringsFileProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.testing.assert
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class StringsFormatProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    importFile = ImportFile("Localizable.strings", importMock)
    importFileDto =
      ImportFileDto(
        "Localizable.strings",
        File("src/test/resources/import/ios/Localizable.strings")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    StringsFileProcessor(fileProcessorContext).process()
    Assertions.assertThat(fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(fileProcessorContext.translations).hasSize(7)
    assertParsed("""welcome_header""", """Hello, {0}""")
    assertParsed("""welcome_sub_header""", """Hello, %s""")
    assertParsed("""another key""", """Dies ist ein weiterer Schlüssel.""")
    assertParsed("""another key " with escaping""", """Dies ist ein weiterer " Schlüssel.""")
    assertParsed("""another key \ with escaping 2""", """Dies ist ein weiterer \ Schlüssel.""")
    assertParsed("""another key with escaping 3\""", """Dies ist ein weiterer Schlüssel\""")
    assertParsed("another key\n\n multiline", "Dies ist ein weiterer\n\nSchlüssel.")

    assertKeyDescription(
      "welcome_sub_header",
      "Welcome header comment" +
        "\nit's a multiline comment\n*/\n\nI cannot trick you!",
    )

    assertKeyDescription("another key", null)
    assertKeyDescription("another key \" with escaping", null)
    assertKeyDescription("another key \\ with escaping 2", null)
    assertKeyDescription(
      "another key\n\n multiline",
      null,
    )
  }

  private fun assertParsed(
    key: String,
    translationText: String,
  ) {
    fileProcessorContext.translations[key]!!.single().text.assert.isEqualTo(translationText)
  }

  private fun assertKeyDescription(
    keyName: String,
    expectedDescription: String?,
  ) {
    val actualDescription = fileProcessorContext.keys[keyName]?.keyMeta?.description
    actualDescription.assert.isEqualTo(expectedDescription)
  }
}
