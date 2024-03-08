package io.tolgee.unit.formats.apple.`in`

import io.tolgee.formats.apple.`in`.strings.StringsFileProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertTranslations
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
    processFile()
    Assertions.assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(mockUtil.fileProcessorContext.translations).hasSize(8)
    assertParsed("""welcome_header""", """Hello, {0}""")
    assertParsed("""welcome_sub_header""", """Hello, %s""")
    assertParsed("""another key""", """Dies ist ein weiterer Schlüssel.""")
    assertParsed("""another key " with escaping""", """Dies ist ein weiterer " Schlüssel.""")
    assertParsed("""another key \ with escaping 2""", """Dies ist ein weiterer \ Schlüssel.""")
    assertParsed("""another key with escaping 3\""", """Dies ist ein weiterer Schlüssel\""")
    assertParsed("another key\n\n multiline", "Dies ist ein weiterer\n\nSchlüssel.")
    assertParsed("A key with \n \n", "Ein Schlüssel mit \n \n")
    assertKeyDescription("A key with \n \n", "this is a comment with newlines \n \n yep")

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

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("unknown", "welcome_header")
      .assertSingle {
        hasText("Hello, %@ {meto}")
      }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("unknown", "welcome_header")
      .assertSingle {
        hasText("Hello, %@ '{'meto'}'")
      }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("unknown", "welcome_header")
      .assertSingle {
        hasText("Hello, {0} '{'meto'}'")
      }
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "values-en/Localizable.string",
      "src/test/resources/import/apple/Localizable_params.strings",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    StringsFileProcessor(mockUtil.fileProcessorContext).process()
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
