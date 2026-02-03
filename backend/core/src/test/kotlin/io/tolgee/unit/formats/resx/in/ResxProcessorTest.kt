package io.tolgee.unit.formats.resx.`in`

import io.tolgee.formats.resx.`in`.ResxProcessor
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

class ResxProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("en.resx", "src/test/resources/import/resx/strings.resx")
  }

  @Test
  fun `returns correct parsed result`() {
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "Title")
      .assertSingle {
        hasText("Classic American Cars")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "HeaderString1")
      .assertSingle {
        hasText("Make")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "HeaderString2")
      .assertSingle {
        hasText("Model")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "HeaderString3")
      .assertSingle {
        hasText("Year")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "HeaderString4")
      .assertSingle {
        hasText("Doors")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "HeaderString5")
      .assertSingle {
        hasText("Cylinders")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test")
      .assertSingle {
        hasText("Text with placeholders {0} and tags </br> and complex tags <p>text</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test2")
      .assertSingle {
        hasText(
          "special \" \\\\ characters\n handling 	 could 耀 be  <value>asdf</value>    interesting " +
            "</value> <p>a</p> lot </data> also",
        )
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test3")
      .assertSingle {
        hasText("<value>asdf</value>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test4")
      .assertSingle {
        hasText("</value>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test5")
      .assertSingle {
        hasText("</data>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test6")
      .assertSingle {
        hasText("</root>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test7")
      .assertSingle {
        hasText("</asdf>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test8")
      .assertSingle {
        hasText("<p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test9")
      .assertSingle {
        hasText("<p>a</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test10")
      .assertSingle {
        hasText("<p>{0}</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test11")
      .assertSingle {
        hasText("Text with placeholders {0} and tags </br> and complex tags <p>text</p>")
      }
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test")
      .assertSingle {
        hasText("Text with placeholders {0} and tags </br> and complex tags <p>text</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test2")
      .assertSinglePlural {
        hasText(
          """
          {num, plural,
          one {Showing '#' item}
          other {Showing '{'num'}' items}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test3")
      .assertSingle {
        hasText("Text with named placeholders {asdf}")
      }
    mockUtil.fileProcessorContext.assertKey("test2") {
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
      .assertTranslations("en", "test")
      .assertSingle {
        hasText("Text with placeholders {0} and tags </br> and complex tags <p>text</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test2")
      .assertSinglePlural {
        hasText(
          """
          {num, plural,
          one {Showing # item}
          other {Showing {num} items}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test3")
      .assertSingle {
        hasText("Text with named placeholders {asdf}")
      }
    mockUtil.fileProcessorContext.assertKey("test2") {
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
      .assertTranslations("en", "test")
      .assertSingle {
        hasText("Text with placeholders {0} and tags </br> and complex tags <p>text</p>")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test2")
      .assertSinglePlural {
        hasText(
          """
          {num, plural,
          one {Showing # item}
          other {Showing {num} items}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext
      .assertTranslations("en", "test3")
      .assertSingle {
        hasText("Text with named placeholders {asdf}")
      }
    mockUtil.fileProcessorContext.assertKey("test2") {
      custom.assert.isNull()
      description.assert.isNull()
    }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "en.resx",
      "src/test/resources/import/resx/strings_icu_everywhere.resx",
      assertBeforeSettingsApplication =
        listOf(
          "Text with placeholders {0} and tags </br> and complex tags <p>text</p>",
          "{num, plural,\n" +
            "one {Showing # item}\n" +
            "other {Showing {num} items}\n" +
            "}",
          "Text with named placeholders {asdf}",
        ),
      assertAfterDisablingConversion =
        listOf(),
      assertAfterReEnablingConversion =
        listOf(),
    )
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "en.resx",
      "src/test/resources/import/resx/strings_icu_everywhere.resx",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    ResxProcessor(mockUtil.fileProcessorContext).process()
  }
}
