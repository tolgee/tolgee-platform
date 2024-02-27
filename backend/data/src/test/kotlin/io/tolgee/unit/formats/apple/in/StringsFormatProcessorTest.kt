package io.tolgee.unit.formats.apple.`in`

import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StringsFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("Localizable.strings", "src/test/resources/import/apple/Localizable.strings")
  }

  @Test
  fun `returns correct parsed result`() {
    StringsFileProcessor(mockUtil.fileProcessorContext).process()
    Assertions.assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(mockUtil.fileProcessorContext.translations).hasSize(7)
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
    mockUtil.fileProcessorContext.translations[key]!!.single().text.assert.isEqualTo(translationText)
  }

  private fun assertKeyDescription(
    keyName: String,
    expectedDescription: String?,
  ) {
    val actualDescription = mockUtil.fileProcessorContext.keys[keyName]?.keyMeta?.description
    actualDescription.assert.isEqualTo(expectedDescription)
  }
}
