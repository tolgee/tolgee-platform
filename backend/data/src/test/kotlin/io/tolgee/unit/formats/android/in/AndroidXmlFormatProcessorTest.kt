package io.tolgee.unit.formats.android.`in`

import io.tolgee.formats.android.`in`.AndroidStringsXmlProcessor
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AndroidXmlFormatProcessorTest {
  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("values-en/strings.xml", "src/test/resources/import/android/strings.xml")
  }

  @Test
  fun `returns correct parsed result`() {
    AndroidStringsXmlProcessor(mockUtil.fileProcessorContext).process()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "app_name")
      .assertSingle {
        hasText("Tolgee test")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "dogs_count")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {# dog}
          other {# dogs}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "string_array[1]")
      .assertSingle {
        hasText("Second item")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "with_spaces")
      .assertSingle {
        hasText("Hello!")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "with_html")
      .assertSingle {
        hasText("<b>Hello!</b>")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "with_xliff_gs")
      .assertSingle {
        hasText(
          "<b>Hello!\n" +
            "            <xliff:g id=\"number\">{0, number}</xliff:g>\n" +
            "        </b>\n" +
            "        <xliff:g id=\"number\">Dont'translate this</xliff:g>",
        )
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertTranslations("en", "with_params")
      .assertSingle {
        hasText("{0, number} {3} {2, number, .00} {3, number, scientific} %+d")
      }
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
  }
}
