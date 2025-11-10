package io.tolgee.unit.formats.json.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.ImportFileMapping
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.json.`in`.JsonFileProcessor
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

class JsonFormatProcessorTest {
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
  fun `returns correct parsed result`() {
    mockUtil.mockIt("example.json", "src/test/resources/import/json/example.json")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("example", "common.save")
    mockUtil.fileProcessorContext.assertTranslations("example", "array[0]")
    mockUtil.fileProcessorContext
      .assertTranslations("example", "array[1]")
      .assertSingle {
        hasText("two")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "array[2]")
      .assertSingle {
        hasText("three")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "a.b.c")
      .assertSingle {
        hasText("This is nested hard.")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "a.b.d[0]")
      .assertSingle {
        hasText("one")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "a.b.d[1]")
      .assertSingle {
        hasText("two")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "a.b.d[2]")
      .assertSingle {
        hasText("three")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "boolean")
      .assertSingle {
        hasText("true")
      }
    mockUtil.fileProcessorContext.keys.assert
      .containsKeys("null")
  }

  @Test
  fun `returns correct parsed result (root array)`() {
    mockUtil.mockIt("example.json", "src/test/resources/import/json/example_root_array.json")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("example", "[0]")
      .assertSingle {
        hasText("item 1")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "[1]")
      .assertSingle {
        hasText("item 2")
      }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one '#' '{'icuParam'}'}
          other {Hello other '{'icuParam'}'}
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
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam}}
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
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam}}
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
      "en.json",
      "src/test/resources/import/json/java.json",
      assertBeforeSettingsApplication =
        listOf(
          "%D this is java {1, number}",
          "%D this is java",
        ),
      assertAfterDisablingConversion =
        listOf(
          "%D this is java %d",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "%D this is java {1, number}",
        ),
    )
  }

  @Test
  fun `respects provided format`() {
    mockUtil.mockIt("en.json", "src/test/resources/import/json/icu.json")
    mockUtil.fileProcessorContext.params =
      SingleStepImportRequest().also {
        it.fileMappings = listOf(ImportFileMapping(fileName = "en.json", format = ImportFormat.JSON_PHP))
      }
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        // it's escaped, because php cannot contain ICU
        hasText("'{'param'}'")
      }
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "en.json",
      "src/test/resources/import/json/example_params.json",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    JsonFileProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
  }
}
