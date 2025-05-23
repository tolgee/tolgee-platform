package io.tolgee.unit.xlsx.`in`

import io.tolgee.formats.xlsx.`in`.XlsxFileProcessor
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

class XlsxFormatProcessorTest {
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
    mockUtil.mockIt("example.xlsx", "src/test/resources/import/xlsx/example.xlsx")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyDeep.inner")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyDeep.inner")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolate")
      .assertSingle {
        hasText("replace this {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolate")
      .assertSingle {
        hasText("nahradit toto {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("replace this {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("nahradit toto {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { the singular}
          other { the plural {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { jednotné číslo}
          other { množné číslo {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "escapedCharacters")
      .assertSingle {
        hasText("this is a \"quote\"")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "escapedCharacters")
      .assertSingle {
        hasText("toto je \"citace\"")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "escapedCharacters2")
      .assertSingle {
        hasText("this is a\nnew line")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "escapedCharacters2")
      .assertSingle {
        hasText("toto je\nnový řádek")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "escapedCharacters3")
      .assertSingle {
        hasText("this is a \\ backslash")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "escapedCharacters3")
      .assertSingle {
        hasText("toto je zpětné \\ lomítko")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "escapedCharacters4")
      .assertSingle {
        hasText("this is a , comma")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "escapedCharacters4")
      .assertSingle {
        hasText("toto je , čárka")
      }
    mockUtil.fileProcessorContext.assertKey("keyPluralSimple") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `returns correct parsed result (semicolon delimiter)`() {
    mockUtil.mockIt("example.xlsx", "src/test/resources/import/xlsx/example_semicolon.xlsx")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyDeep.inner")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyDeep.inner")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolate")
      .assertSingle {
        hasText("replace this {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolate")
      .assertSingle {
        hasText("nahradit toto {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("replace this {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("nahradit toto {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { the singular}
          other { the plural {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { jednotné číslo}
          other { množné číslo {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("keyPluralSimple") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `returns correct parsed result (tab delimiter)`() {
    mockUtil.mockIt("example.xlsx", "src/test/resources/import/xlsx/example_tab.xlsx")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyDeep.inner")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyDeep.inner")
      .assertSingle {
        hasText("hodnota")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolate")
      .assertSingle {
        hasText("replace this {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolate")
      .assertSingle {
        hasText("nahradit toto {value}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("replace this {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("nahradit toto {value, number}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { the singular}
          other { the plural {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one { jednotné číslo}
          other { množné číslo {value}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("keyPluralSimple") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("Ahoj {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Hello one '#' '{'icuParam'}'}
          other {Hello other '{'icuParam'}'}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Ahoj jedno '#' '{'icuParam'}'}
          other {Ahoj jiné '{'icuParam'}'}
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
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("Ahoj {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Ahoj jedno # {icuParam}}
          other {Ahoj jiné {icuParam}}
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
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "key")
      .assertSingle {
        hasText("Ahoj {icuPara}")
      }
    mockUtil.fileProcessorContext.assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Hello one # {icuParam}}
          other {Hello other {icuParam}}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertTranslations("cs", "plural")
      .assertSinglePlural {
        hasText(
          """
          {icuPara, plural,
          one {Ahoj jedno # {icuParam}}
          other {Ahoj jiné {icuParam}}
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
      "import.xlsx",
      "src/test/resources/import/xlsx/placeholder_conversion.xlsx",
      assertBeforeSettingsApplication =
        listOf(
          "this is xlsx {0, number}",
          "this is xlsx",
          "toto je xlsx {0, number}",
          "toto je xlsx",
        ),
      assertAfterDisablingConversion =
        listOf(
          "this is xlsx %d",
          "toto je xlsx %d",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "this is xlsx {0, number}",
          "toto je xlsx {0, number}",
        ),
    )
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "import.xlsx",
      "src/test/resources/import/xlsx/example_params.xlsx",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    XlsxFileProcessor(mockUtil.fileProcessorContext).process()
  }
}
