package io.tolgee.unit.formats.apple.`in`

import io.tolgee.formats.apple.`in`.xliff.AppleXliffFileProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.testing.assert
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.hasKeyDescription
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AppleXliffFormatProcessorTest {
  private val xmlEventReader: XMLEventReader
    get() {
      val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
      return inputFactory.createXMLEventReader(mockUtil.importFileDto.data.inputStream())
    }

  private val parsed get() = XliffParser(xmlEventReader).parse()

  lateinit var mockUtil: FileProcessorContextMockUtil

  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("cs.xliff", "src/test/resources/import/apple/cs.xliff")
  }

  @Test
  fun `returns correct parsed result`() {
    AppleXliffFileProcessor(mockUtil.fileProcessorContext, parsed).process()
    mockUtil.fileProcessorContext.translations.size.assert.isEqualTo(6)
    mockUtil.fileProcessorContext.assertLanguagesCount(2)
    mockUtil.fileProcessorContext.assertTranslations("en", "Dogs %lld")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          zero {No dogs here!}
          one {One dog is here!}
          other {# dogs here}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
        hasKeyDescription("The count of dogs in the app")
      }

    mockUtil.fileProcessorContext.assertTranslations("en", "Order %lld")
      .assertSinglePlural {
        isPluralOptimized()
      }

    mockUtil.fileProcessorContext.assertTranslations("en", "key").assertSingle {
      hasText("Hello!")
    }
  }
}
