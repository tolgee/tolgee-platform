package io.tolgee.unit.formats.compose.`in`

import io.tolgee.formats.xmlResources.`in`.XmlResourcesProcessor
import io.tolgee.testing.assert
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.custom
import io.tolgee.util.description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ComposeXmlFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("values-en/strings.xml", "src/test/resources/import/composeMultiplatform/strings.xml")
  }

  @Test
  fun `returns correct parsed result`() {
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "app_name")
      .assertSingle {
        hasText("Tolgee test")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogs_count")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {# dog}
          other {# dogs}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[1]")
      .assertSingle {
        hasText("Second item")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_spaces")
      .assertSingle {
        hasText("Hello!")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_html")
      .assertSingle {
        hasText("<b>Hello!</b>")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_params")
      .assertSingle {
        hasText("{0, number} {3} {2, number, .00} {3, number, scientific} %5${'$'}+d")
      }
    mockUtil.fileProcessorContext.assertKey("app_name") {
      custom.assert.isNull()
      description.assert.isEqualTo("This is a comment")
    }
    mockUtil.fileProcessorContext.assertKey("dogs_count") {
      custom.assert.isNull()
      description.assert.isEqualTo("This is a comment above a plural")
    }
    mockUtil.fileProcessorContext.assertKey("string_array") {
      custom.assert.isNull()
      description.assert.isEqualTo("This is a comment above an array item #2")
    }
    mockUtil.fileProcessorContext.assertKey("with_spaces") {
      custom.assert.isNull()
      description.assert.isEqualTo("and only the last one will be kept")
    }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogs_count")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {%1${'$'}d dog %2${'$'}s '{'escape'}'}
          other {%1${'$'}d dogs %2${'$'}s}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item %1${'$'}d {escape}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_params")
      .assertSingle {
        hasText("%1${'$'}d %4${'$'}s %3${'$'}.2f %4${'$'}e %5${'$'}+d {escape}")
      }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogs_count")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {%1${'$'}d dog %2${'$'}s '{'escape'}'}
          other {%1${'$'}d dogs %2${'$'}s}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item %1${'$'}d '{'escape'}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_params")
      .assertSingle {
        hasText("%1${'$'}d %4${'$'}s %3${'$'}.2f %4${'$'}e %5${'$'}+d '{'escape'}'")
      }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogs_count")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {# dog {1} '{'escape'}'}
          other {# dogs {1}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item {0, number} '{'escape'}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "string_array[1]")
      .assertSingle {
        hasText("Second item {0, number}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "with_params")
      .assertSingle {
        hasText("{0, number} {3} {2, number, .00} {3, number, scientific} %5${'$'}+d '{'escape'}'")
      }
    mockUtil.fileProcessorContext.assertKey("dogs_count") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "values-en/strings.xml",
      "src/test/resources/import/android/strings_params_everywhere.xml",
      assertBeforeSettingsApplication =
        listOf(
          "{0, plural,\none {# dog {1} '{'escape'}'}\nother {# dogs {1}}\n}",
          "First item {0, number} '{'escape'}'",
          "Second item {0, number}",
          "{0, number} {3} {2, number, .00} {3, number, scientific} %+d '{'escape'}'",
        ),
      assertAfterDisablingConversion =
        listOf(
          "{value, plural,\none {%d dog %s '{'escape'}'}\nother {%d dogs %s}\n}",
          "First item %d '{'escape'}'",
          "Second item %d",
          "%d %4\$s %.2f %e %+d '{'escape'}'",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{0, plural,\none {# dog {1} '{'escape'}'}\nother {# dogs {1}}\n}",
          "First item {0, number} '{'escape'}'",
          "Second item {0, number}",
          "{0, number} {3} {2, number, .00} {3, number, scientific} %+d '{'escape'}'",
        ),
    )
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "values-en/strings.xml",
      "src/test/resources/import/composeMultiplatform/strings_params_everywhere.xml",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    XmlResourcesProcessor(mockUtil.fileProcessorContext).process()
  }
}
