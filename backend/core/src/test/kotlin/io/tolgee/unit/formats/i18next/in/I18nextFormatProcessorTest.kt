package io.tolgee.unit.formats.i18next.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

class I18nextFormatProcessorTest {
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
    mockUtil.mockIt("example.json", "src/test/resources/import/i18next/example.json")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("example", "key")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyDeep.inner")
      .assertSingle {
        hasText("value")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyNesting")
      .assertSingle {
        hasText("reuse ${'$'}t(keyDeep.inner) (is not supported)")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolate")
      .assertSingle {
        hasText("replace this {value}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolateUnescaped")
      .assertSingle {
        hasText("replace this '{{'- value'}}' (we don't support unescaped flag)")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolateUnescapedMultiple")
      .assertSingle {
        hasText("replace this '{{'- value'}}' and also don't escape '{{'- value'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolateUnescapedConflict")
      .assertSingle {
        hasText("replace this '{{'- value'}}' but escape this {value}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolateUnescapedConflictInverted")
      .assertSingle {
        hasText("replace this {value} but don't escape this '{{'- value'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyInterpolateWithFormatting")
      .assertSingle {
        hasText("replace this {value, number} (only number is supported)")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyContext_male")
      .assertSingle {
        hasText("the male variant (is parsed as normal key and context is ignored)")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyContext_female")
      .assertSingle {
        hasText("the female variant (is parsed as normal key and context is ignored)")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyWithArrayValue[0]")
      .assertSingle {
        hasText("multipe")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyWithArrayValue[1]")
      .assertSingle {
        hasText("things")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyWithObjectValue.valueA")
      .assertSingle {
        hasText("valueA")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyWithObjectValue.valueB")
      .assertSingle {
        hasText("more text")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyPluralSimple")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {the singular (is parsed as plural under one key - keyPluralSimple)}
          other {the plural (is parsed as plural under one key - keyPluralSimple)}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "keyPluralMultipleEgArabic")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {the plural form 1}
          two {the plural form 2}
          few {the plural form 3}
          many {the plural form 4}
          other {the plural form 5}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertKey("keyPluralSimple") {
      custom.assert.isNull()
      description.assert.isNull()
    }
    mockUtil.fileProcessorContext.assertKey("keyPluralMultipleEgArabic") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `returns correct parsed result for more complex file`() {
    mockUtil.mockIt("example.json", "src/test/resources/import/i18next/example2.json")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("example", "note")
      .assertSingle {
        hasText("Most features in this example are not supported by the tolgee yet")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.key")
      .assertSingle {
        hasText("Hello World")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.interpolation_example")
      .assertSingle {
        hasText("Hello {name}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.plural_example")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {You have one message}
          other {You have # messages}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.invalid_plural_example")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {You have one message}
          many {You have # messages (translation is missing 'other' form)}
          other {}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.context_example_male")
      .assertSingle {
        hasText("He is a teacher")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.context_example_female")
      .assertSingle {
        hasText("She is a teacher")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.nested_example")
      .assertSingle {
        hasText("This is a {type} message")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.type")
      .assertSingle {
        hasText("nested")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.formatted_value")
      .assertSingle {
        hasText("The price is '{{'value, currency'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.formatted_value_with_params")
      .assertSingle {
        hasText("The price is '{{'value, currency(USD)'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.intlNumberWithOptions")
      .assertSingle {
        hasText("Some '{{'val, number(minimumFractionDigits: 2)'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.intlList")
      .assertSingle {
        hasText("A list of '{{'val, list'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.array_example[0]")
      .assertSingle {
        hasText("Apples")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.array_example[1]")
      .assertSingle {
        hasText("Oranges")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.array_example[2]")
      .assertSingle {
        hasText("Bananas")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.select_example.morning")
      .assertSingle {
        hasText("Good morning")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.select_example.afternoon")
      .assertSingle {
        hasText("Good afternoon")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.select_example.evening")
      .assertSingle {
        hasText("Good evening")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.multiline_example")
      .assertSingle {
        hasText("This is line one.\nThis is line two.")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.gender_with_plural_male")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {He has one cat}
          other {He has # cats}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.gender_with_plural_female")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {She has one cat}
          other {She has # cats}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.rich_text_example")
      .assertSingle {
        hasText("<strong>Welcome</strong> to our application!")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.json_value_example.key")
      .assertSingle {
        hasText("This is a value inside a JSON object")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.language_switch.en")
      .assertSingle {
        hasText("English")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.language_switch.es")
      .assertSingle {
        hasText("Spanish")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("example", "translation.language_switch.fr")
      .assertSingle {
        hasText("French")
      }
    mockUtil.fileProcessorContext.assertKey("translation.plural_example") {
      custom.assert.isNull()
      description.assert.isNull()
    }
    mockUtil.fileProcessorContext.assertKey("translation.invalid_plural_example") {
      custom.assert.isNull()
      description.assert.isNull()
    }
    mockUtil.fileProcessorContext.assertKey("translation.gender_with_plural_male") {
      custom.assert.isNull()
      description.assert.isNull()
    }
    mockUtil.fileProcessorContext.assertKey("translation.gender_with_plural_female") {
      custom.assert.isNull()
      description.assert.isNull()
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
        hasText("Hello {{icuPara}}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Hello one '#' '{{'icuParam'}}'}
          other {Hello other '{{'icuParam'}}'}
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
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "key")
      .assertSingle {
        hasText("Hello '{{'icuPara'}}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Hello one '#' '{{'icuParam'}}'}
          other {Hello other '{{'icuParam'}}'}
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
        hasText("Hello {icuPara}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "plural")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {Hello one '#' {icuParam}}
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
      "src/test/resources/import/i18next/simple.json",
      assertBeforeSettingsApplication =
        listOf(
          "'{{'value, currency'}}' this is i18next {count, number}",
          "'{{'value, currency'}}' this is i18next",
        ),
      assertAfterDisablingConversion =
        listOf(
          "'{{'value, currency'}}' this is i18next '{{'count, number'}}'",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "'{{'value, currency'}}' this is i18next {count, number}",
        ),
    )
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "en.json",
      "src/test/resources/import/i18next/example_params.json",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    JsonFileProcessor(mockUtil.fileProcessorContext, jacksonObjectMapper()).process()
  }
}
