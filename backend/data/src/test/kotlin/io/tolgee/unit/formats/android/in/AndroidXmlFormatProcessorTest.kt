package io.tolgee.unit.formats.android.`in`

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.android.`in`.AndroidStringsXmlProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class AndroidXmlFormatProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    val fileName = "values-en/strings.xml"
    importFile = ImportFile(fileName, importMock)
    importFileDto =
      ImportFileDto(
        fileName,
        File("src/test/resources/import/android/strings.xml")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  @Test
  fun `returns correct parsed result`() {
    AndroidStringsXmlProcessor(fileProcessorContext).process()
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "app_name")
      .assertSingle {
        hasText("Tolgee test")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "dogs_count")
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
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "string_array[0]")
      .assertSingle {
        hasText("First item")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "string_array[1]")
      .assertSingle {
        hasText("Second item")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "with_spaces")
      .assertSingle {
        hasText("Hello!")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "with_html")
      .assertSingle {
        hasText("<b>Hello!</b>")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "with_xliff_gs")
      .assertSingle {
        hasText(
          "<b>Hello!\n            <xliff:g id=\"number\">{0, number}</xliff:g>\n        </b>\n        <xliff:g id=\"number\">Dont'translate this</xliff:g>",
        )
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "with_params")
      .assertSingle {
        hasText("{0, number} {3} {2, number, .00} {3, number, scientific} %+d")
      }
    fileProcessorContext.assertLanguagesCount(1)
  }
}
