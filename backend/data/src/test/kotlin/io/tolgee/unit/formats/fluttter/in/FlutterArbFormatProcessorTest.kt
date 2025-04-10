package io.tolgee.unit.formats.fluttter.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.formats.flutter.`in`.FlutterArbFileProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.custom
import io.tolgee.util.customEquals
import io.tolgee.util.description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FlutterArbFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("app_en.arb", "src/test/resources/import/flutter/app_en.arb")
  }

  // This is how to generate the test:
  // 1. run the test in debug mode
  // 2. copy the result of calling: generateTestsForImportResult(mockUtil.fileProcessorContext) from the debug window
  @Test
  fun `returns correct parsed result`() {
    FlutterArbFileProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "helloWorld")
      .assertSingle {
        hasText("Hello World!")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogsCount")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {I have one dog.}
          other {I have {count} dogs.}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "simpleDogCount")
      .assertSingle {
        hasText("Dogs count: {count}")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertKey("helloWorld") {
      custom.assert.isNull()
      description.assert.isEqualTo("The conventional newborn programmer greeting")
    }
    mockUtil.fileProcessorContext.assertKey("dogsCount") {
      customEquals(
        """
        {
            "_flutterArbPlaceholders" : {
              "count" : {
                "type" : "int",
                "optionalParameters" : {
                  "decimalDigits" : 1
                }
              }
            }
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The conventional newborn programmer greeting")
    }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "helloWorld")
      .assertSingle {
        hasText("Hello World! {name}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogsCount")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {I have one dog.}
          other {I have '{'count'}' dogs.}
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
      .assertTranslations("en", "helloWorld")
      .assertSingle {
        hasText("Hello World! {name}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogsCount")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {I have one dog.}
          other {I have {count} dogs.}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("dogsCount") {
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
      .assertTranslations("en", "helloWorld")
      .assertSingle {
        hasText("Hello World! {name}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "dogsCount")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {I have one dog.}
          other {I have {count} dogs.}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "values-en/app_en.arb",
      "src/test/resources/import/flutter/app_en_params.arb",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    FlutterArbFileProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
  }
}
