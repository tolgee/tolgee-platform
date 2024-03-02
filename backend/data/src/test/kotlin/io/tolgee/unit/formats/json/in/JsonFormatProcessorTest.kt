package io.tolgee.unit.formats.json.`in`

import io.tolgee.formats.json.`in`.JsonFileProcessor
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertTranslations
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
    JsonFileProcessor(mockUtil.fileProcessorContext).process()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("example", "common.save")
    mockUtil.fileProcessorContext.assertTranslations("example", "array[0]")
    mockUtil.fileProcessorContext.assertTranslations("example", "array[1]")
      .assertSingle {
        hasText("two")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "array[2]")
      .assertSingle {
        hasText("three")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "a.b.c")
      .assertSingle {
        hasText("This is nested hard.")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "a.b.d[0]")
      .assertSingle {
        hasText("one")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "a.b.d[1]")
      .assertSingle {
        hasText("two")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "a.b.d[2]")
      .assertSingle {
        hasText("three")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "boolean")
      .assertSingle {
        hasText("true")
      }
    mockUtil.fileProcessorContext.keys.assert.containsKeys("null")
  }

  @Test
  fun `returns correct parsed result (root array)`() {
    mockUtil.mockIt("example.json", "src/test/resources/import/json/example_root_array.json")
    JsonFileProcessor(mockUtil.fileProcessorContext).process()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("example", "[0]")
      .assertSingle {
        hasText("item 1")
      }
    mockUtil.fileProcessorContext.assertTranslations("example", "[1]")
      .assertSingle {
        hasText("item 2")
      }
  }
}
