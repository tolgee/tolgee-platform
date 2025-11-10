package io.tolgee.unit.formats.properties.`in`

import io.tolgee.dtos.request.ImportFileMapping
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.formats.importCommon.ImportFormat
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

  // This is how to generate the test:
  // 1. run the test in debug mode
  // 2. copy the result of calling:
  // io.tolgee.unit.util.generateTestsForImportResult(mockUtil.fileProcessorContext)
  // from the debug window
  @Test
  fun `basic cases`() {
    mockUtil.mockIt(
      "messages_en.properties",
      "src/test/resources/import/properties/example.properties",
    )
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "key1")
      .assertSingle {
        hasText("Duplicated")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "escaping test")
      .assertSingle {
        hasText("Escaping = \\ = \n new line \n = = \"")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "array")
      .assertSingle {
        hasText("1, 2, 3")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "with.dots.s")
      .assertSingle {
        hasText("Hey")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "number")
      .assertSingle {
        hasText("1")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "boolean")
      .assertSingle {
        hasText("true")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "with_commnet")
      .assertSingle {
        hasText("with comment")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("messages_en", "with_commnet_2")
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
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
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
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
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
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara} '{escaped}',")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
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
  fun `placeholder conversion does nothing`() {
    PlaceholderConversionTestHelper.testFile(
      "en.properties",
      "src/test/resources/import/properties/example_params.properties",
      assertBeforeSettingsApplication =
        listOf(
          "Hello {icuPara} '{escaped}',",
          "{count, plural,\none {Hello one # {icuParam}}\nother {Hello other {icuParam} '{escaped}'}\n}",
        ),
      assertAfterDisablingConversion =
        listOf(),
      assertAfterReEnablingConversion =
        listOf(),
    )
  }

  @Test
  fun `respects provided format`() {
    mockUtil.mockIt("en.properties", "src/test/resources/import/properties/icu.properties")
    mockUtil.fileProcessorContext.params =
      SingleStepImportRequest().also {
        it.fileMappings =
          listOf(ImportFileMapping(fileName = "en.properties", format = ImportFormat.PROPERTIES_JAVA))
      }
    processFile()
    // it's escaped because ICU doesn't php doesn't contain ICU
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key1")
      .assertSingle {
        hasText("Param '{'hello'}'")
      }
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
