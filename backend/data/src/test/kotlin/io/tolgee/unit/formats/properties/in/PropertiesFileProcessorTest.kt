package io.tolgee.unit.formats.properties.`in`

import io.tolgee.formats.properties.`in`.PropertiesFileProcessor
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

class PropertiesFileProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `basic cases`() {
    mockUtil.mockIt(
      "messages_en.properties",
      "src/test/resources/import/properties/example.properties",
    )
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "key1")
      .assertSingle {
        hasText("Duplicated")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "escaping test")
      .assertSingle {
        hasText("Escaping = \\ = \n new line \n = = \"")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "array")
      .assertSingle {
        hasText("1, 2, 3")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "with.dots.s")
      .assertSingle {
        hasText("Hey")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "number")
      .assertSingle {
        hasText("1")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "boolean")
      .assertSingle {
        hasText("true")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "with_commnet")
      .assertSingle {
        hasText("with comment")
      }
    mockUtil.fileProcessorContext.assertTranslations("messages_en", "with_commnet_2")
      .assertSingle {
        hasText("with comment")
      }
    mockUtil.fileProcessorContext.assertKey("with_commnet") {
      custom.assert.isNull()
      description.assert.isEqualTo("A commnet")
    }
    mockUtil.fileProcessorContext.assertKey("with_commnet_2") {
      custom.assert.isNull()
      description.assert.isEqualTo("A commnet")
    }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one '#' '{'icuParam'}'}
          other {Hello other '{'icuParam'}' '''{'escaped'}'''}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("plural") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam} '{escaped}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("plural") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam} '{escaped}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("plural") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "en.properties",
      "src/test/resources/import/properties/example_params.properties",
      assertBeforeSettingsApplication =
        listOf(
          "Hi {0, number} '{'icuParam'}'",
          "{0, plural,\none {Hallo {0, number} '{'icuParam'}'}\nother {Hallo {0, number} '{'icuParam'}'}\n}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "Hi %d '{'icuParam'}'",
          "{0, plural,\none {Hallo %d '{'icuParam'}'}\nother {Hallo %d '{'icuParam'}'}\n}",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "Hi {0, number} '{'icuParam'}'",
          "{0, plural,\none {Hallo {0, number} '{'icuParam'}'}\nother {Hallo {0, number} '{'icuParam'}'}\n}",
        ),
    )
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "en.properties",
      "src/test/resources/import/properties/example_params.properties",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    PropertiesFileProcessor(mockUtil.fileProcessorContext).process()
  }
}
