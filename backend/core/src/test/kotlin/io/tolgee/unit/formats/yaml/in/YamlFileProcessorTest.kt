package io.tolgee.unit.formats.yaml.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.tolgee.dtos.request.ImportFileMapping
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.yaml.`in`.YamlFileProcessor
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertTranslations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class YamlFileProcessorTest {
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
  fun `returns correct parsed for spring icu`() {
    mockUtil.mockIt("en.yml", "src/test/resources/import/yaml/icu.yaml")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "relations")
      .assertSingle {
        hasText("{count, plural, one {# relace} few {# relace} many {# relace} other {# relací}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.edit.heading")
      .assertSingle {
        hasText("Upravit redakci")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.edit.title")
      .assertSingle {
        hasText("Upravit redakci")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.empty")
      .assertSingle {
        hasText("Žádné opravy k ukázání.")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.heading")
      .assertSingle {
        hasText("Seznam oprav")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.title")
      .assertSingle {
        hasText("Seznam oprav {hello}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: {param1} a {param2}")
      }
  }

  // This is how to generate the test:
  // 1. run the test in debug mode
  // 2. copy the result of calling:
  // io.tolgee.unit.util.generateTestsForImportResult(mockUtil.fileProcessorContext)
  // from the debug window
  @Test
  fun `returns correct parsed for spring java`() {
    mockUtil.mockIt("en.yml", "src/test/resources/import/yaml/java.yaml")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "relations")
      .assertSingle {
        hasText("{0, number} relací")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.edit.heading")
      .assertSingle {
        hasText("Upravit redakci")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.edit.title")
      .assertSingle {
        hasText("Upravit redakci")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.empty")
      .assertSingle {
        hasText("Žádné opravy k ukázání.")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.heading")
      .assertSingle {
        hasText("Seznam oprav")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "redactions.index.title")
      .assertSingle {
        hasText("Seznam oprav {0}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: {0} {1, number}")
      }
  }

  // This is how to generate the test:
  // 1. run the test in debug mode
  // 2. copy the result of calling:
  // io.tolgee.unit.util.generateTestsForImportResult(mockUtil.fileProcessorContext)
  // from the debug window
  @Test
  fun `returns correct parsed for unknown`() {
    mockUtil.mockIt("en.yml", "src/test/resources/import/yaml/unknown.yaml")
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "not-valid-bcp47-tag!.relations.part_of_relations")
      .assertSingle {
        hasText("Some text without params")
      }
  }

  @Test
  fun `respects provided format`() {
    mockUtil.mockIt("en.yaml", "src/test/resources/import/yaml/icu.yaml")
    mockUtil.fileProcessorContext.params =
      SingleStepImportRequest().also {
        it.fileMappings =
          listOf(ImportFileMapping(fileName = "en.yaml", format = ImportFormat.YAML_PHP))
      }
    processFile()
    // it's escaped because ICU doesn't php doesn't contain ICU
    mockUtil.fileProcessorContext
      .assertTranslations("en", "some_text_with_params")
      .assertSingle {
        hasText("Toto je text s parametry: '{'param1'}' a '{'param2'}'")
      }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "en.yaml",
      "src/test/resources/import/yaml/java.yaml",
      assertBeforeSettingsApplication =
        listOf(
          "{0, number} relací",
          "Upravit redakci",
          "Upravit redakci",
          "Žádné opravy k ukázání.",
          "Seznam oprav",
          "Seznam oprav {0}",
          "Toto je text s parametry: {0} {1, number}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "%d relací",
          "Seznam oprav %s",
          "Toto je text s parametry: %s %d",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "{0, number} relací",
          "Seznam oprav {0}",
          "Toto je text s parametry: {0} {1, number}",
        ),
    )
  }

  private fun processFile() {
    YamlFileProcessor(mockUtil.fileProcessorContext, ObjectMapper(YAMLFactory())).process()
  }
}
