package io.tolgee.unit.formats.apple.`in`

import StringsdictFileProcessor
import io.tolgee.testing.assert
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.custom
import io.tolgee.util.description
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StringsdictFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.stringsdict", "src/test/resources/import/apple/example.stringsdict")
  }

  @Test
  fun `returns correct parsed result`() {
    processFile()
    Assertions.assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    Assertions.assertThat(mockUtil.fileProcessorContext.translations).hasSize(2)
    mockUtil.fileProcessorContext.translations["what-a-key-plural"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Peter has # dog}\n" +
        "other {Peter hase # dogs}\n" +
        "}",
    )
    mockUtil.fileProcessorContext.translations["what-a-key-plural-2"]!![0].text.assert.isEqualTo(
      "{0, plural,\n" +
        "one {Lucy has %la '{'dog'}'}\n" +
        "other {Lucy has %la '{'dogs'}'}\n" +
        "}",
    )
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("unknown", "what-a-key-plural")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Peter has %lld dog '{'meto'}'}
          other {Peter hase %lld dogs '{'meto'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("unknown", "what-a-key-plural")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Peter has %lld dog '{'meto'}'}
          other {Peter hase %lld dogs '{'meto'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("unknown", "what-a-key-plural")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {Peter has # dog '{'meto'}'}
          other {Peter hase # dogs '{'meto'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("what-a-key-plural") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "values-en/Localizable.stringsdict",
      "src/test/resources/import/apple/Localizable_params.stringsdict",
      assertBeforeSettingsApplication =
        listOf(
          "{0, plural,\none {Peter has # dog '{'meto'}'}\nother {Peter hase # dogs '{'meto'}'}\n}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "{value, plural,\none {Peter has %lld dog '{'meto'}'}\nother {Peter hase %lld dogs '{'meto'}'}\n}",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{0, plural,\none {Peter has # dog '{'meto'}'}\nother {Peter hase # dogs '{'meto'}'}\n}",
        ),
    )
  }

  private fun processFile() {
    StringsdictFileProcessor(mockUtil.fileProcessorContext).process()
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "values-en/Localizable.stringsdict",
      "src/test/resources/import/apple/Localizable_params.stringsdict",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }
}
