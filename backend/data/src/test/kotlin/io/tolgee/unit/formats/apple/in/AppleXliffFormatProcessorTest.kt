package io.tolgee.unit.formats.apple.`in`

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.apple.`in`.AppleXliffFileProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.testing.assert
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.hasKeyDescription
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AppleXliffFormatProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  private val xmlEventReader: XMLEventReader
    get() {
      val inputFactory: XMLInputFactory = XMLInputFactory.newDefaultFactory()
      return inputFactory.createXMLEventReader(importFileDto.data.inputStream())
    }

  private val parsed get() = XliffParser(xmlEventReader).parse()

  @BeforeEach
  fun setup() {
    importMock = mock()
    val fileName = "cs.xliff"
    importFile = ImportFile(fileName, importMock)
    importFileDto =
      ImportFileDto(
        fileName,
        File("src/test/resources/import/apple/cs.xliff")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    AppleXliffFileProcessor(fileProcessorContext, parsed).process()
    fileProcessorContext.translations.size.assert.isEqualTo(6)
    fileProcessorContext.assertLanguagesCount(2)
    fileProcessorContext.assertTranslations("en", "Dogs %lld")
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

    fileProcessorContext.assertTranslations("en", "Order %lld")
      .assertSinglePlural {
        isPluralOptimized()
      }

    fileProcessorContext.assertTranslations("en", "key").assertSingle {
      hasText("Hello!")
    }
  }
}
