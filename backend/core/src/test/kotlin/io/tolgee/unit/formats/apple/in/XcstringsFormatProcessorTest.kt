package io.tolgee.unit.formats.apple.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.formats.apple.`in`.xcstrings.XcstringsFileProcessor
import io.tolgee.testing.assert
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class XcstringsFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.xcstrings", "src/test/resources/import/apple/example.xcstrings")
  }

  @Test
  fun `returns correct parsed result`() {
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(2) // en, fr
    mockUtil.fileProcessorContext
      .assertTranslations("en", "hello-world")
      .assertSingle {
        hasText("Hello, World!")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("fr", "hello-world")
      .assertSingle {
        hasText("Bonjour le monde!")
      }
  }

  @Test
  fun `handles plural translations correctly`() {
    processFile()
    mockUtil.fileProcessorContext
      .assertTranslations("en", "messages-count")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {You have # message}
          other {You have # messages}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "messages-count")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {You have %lld message}
          other {You have %lld messages}
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
      .assertTranslations("en", "messages-count")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {You have # message}
          other {You have # messages}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
  }

  @Test
  fun `preserves metadata`() {
    processFile()
    mockUtil.fileProcessorContext.assertKey("hello-world") {
      description.assert.isEqualTo("A greeting message")
    }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "example.xcstrings",
      "src/test/resources/import/apple/example_params.xcstrings",
      assertBeforeSettingsApplication =
        listOf(
          "{0, plural,\none {You have # message}\nother {You have # messages}\n}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "{value, plural,\none {You have %lld message}\nother {You have %lld messages}\n}",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{0, plural,\none {You have # message}\nother {You have # messages}\n}",
        ),
    )
  }

  @Test
  fun `import with ICU escaping (disabled ICU)`() {
    mockUtil.mockIt(
      "example.xcstrings",
      "src/test/resources/import/apple/example_params_escaped.xcstrings",
      convertPlaceholders = false,
      projectIcuPlaceholdersEnabled = false,
    )
    processFile()
    mockUtil.fileProcessorContext
      .assertTranslations("en", "welcome-message-escaped")
      .assertSingle {
        hasText("Hello, %@ {meto}")
      }
  }

  private fun processFile() {
    XcstringsFileProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "example.xcstrings",
      "src/test/resources/import/apple/example_params.xcstrings",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }
}
