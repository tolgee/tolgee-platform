package io.tolgee.unit.formats.yaml.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.tolgee.formats.yaml.`in`.YamlFileProcessor
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

class RubyYamlFileProcessorTest {
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
  fun `returns correct parsed result for ruby`() {
    mockUtil.mockIt("en.yml", "src/test/resources/import/yaml/ruby.yaml")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "relations.part_of_relations")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {# relace}
          few {# relace}
          many {# relace}
          other {# relací}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.edit.heading")
      .assertSingle {
        hasText("Upravit redakci {count, number}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.index.title")
      .assertSingle {
        hasText("Seznam oprav {0, number} {1} {2, number, .00}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: {param1} a {param2}")
      }
    mockUtil.fileProcessorContext.assertKey("relations.part_of_relations") {
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
      .assertTranslations("cs", "relations.part_of_relations")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {%'{'count'}' relace}
          few {%'{'count'}' relace}
          many {%'{'count'}' relace}
          other {%'{'count'}' relací}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.edit.heading")
      .assertSingle {
        hasText("Upravit redakci %<count>d")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.index.title")
      .assertSingle {
        hasText("Seznam oprav %d %s %.2f")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: %{param1} a %{param2}")
      }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "relations.part_of_relations")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {%'{'count'}' relace}
          few {%'{'count'}' relace}
          many {%'{'count'}' relace}
          other {%'{'count'}' relací}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.edit.heading")
      .assertSingle {
        hasText("Upravit redakci %<count>d")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "redactions.index.title")
      .assertSingle {
        hasText("Seznam oprav %d %s %.2f")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("cs", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: %'{'param1'}' a %'{'param2'}'")
      }
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "values-en/strings.xml",
      "src/test/resources/import/yaml/ruby.yaml",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "en.yml",
      "src/test/resources/import/yaml/ruby.yaml",
      assertBeforeSettingsApplication =
        listOf(
          "{count, plural,\none {# relace}\nfew {# relace}\nmany {# relace}\nother {# relací}\n}",
          "Upravit redakci {count, number}",
          "Seznam oprav {0, number} {1} {2, number, .00}",
          "Toto je text s parametry: {param1} a {param2}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "{value, plural,\none {%'{'count'}' relace}\nfew {%'{'count'}' relace}\nmany {%'{'count'}' relace}\n" +
            "other {%'{'count'}' relací}\n}",
          "Upravit redakci %<count>d",
          "Seznam oprav %d %s %.2f",
          "Toto je text s parametry: %'{'param1'}' a %'{'param2'}'",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{count, plural,\none {# relace}\nfew {# relace}\nmany {# relace}\nother {# relací}\n}",
          "Upravit redakci {count, number}",
          "Seznam oprav {0, number} {1} {2, number, .00}",
          "Toto je text s parametry: {param1} a {param2}",
        ),
    )
  }

  private fun processFile() {
    YamlFileProcessor(mockUtil.fileProcessorContext, ObjectMapper(YAMLFactory())).process()
  }
}
