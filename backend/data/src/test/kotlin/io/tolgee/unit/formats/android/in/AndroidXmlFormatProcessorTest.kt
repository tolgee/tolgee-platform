package io.tolgee.unit.formats.android.`in`

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.android.`in`.AndroidStringsXmlProcessor
import io.tolgee.formats.xliff.`in`.parser.XliffParser
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
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class AndroidXmlFormatProcessorTest {
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

// This is how to generate the test
//  val translations = fileProcessorContext.translations
//  val languageCount = fileProcessorContext.languages.size
//  val size = translations.size
//  val code = StringBuilder()
//  val i = { i: Int -> (1..i).joinToString("") { "  " } }
//  code.appendLine("${i(2)}fileProcessorContext.assertLanguagesCount(${languageCount})")
//  fileProcessorContext.translations.forEach { (keyName, translations) ->
//    val byLanguage = translations.groupBy { it.language.name }
//    byLanguage.forEach { (language, translations) ->
//      code.appendLine("""${i(2)}fileProcessorContext.assertTranslations("$language", "$keyName")""")
//      val translation = translations.singleOrNull() ?: return@forEach
//      if(translation.isPlural){
//        code.appendLine("""${i(3)}.assertSinglePlural {""")
//        code.appendLine("""${i(4)}hasText(""")
//        code.appendLine("""${i(5)}${"\"\"\""}""")
//        code.appendLine(i(5) + translation.text.replace("\n", "\n${i(5)}"))
//        code.appendLine("""${i(5)}${"\"\"\""}.trimIndent()""")
//        code.appendLine("""${i(4)})""")
//        code.appendLine("""${i(4)}isPluralOptimized()""")
//        code.appendLine("""${i(3)}}""")
//      }else{
//        code.appendLine("""${i(3)}.assertSingle {""")
//        code.appendLine("""${i(4)}hasText("${translation.text.replace("\n","\\n").replace("\"", "\\\"")}")""")
//        code.appendLine("""${i(3)}}""")
//
//      }
//      code.appendLine("${i(2)}fileProcessorContext.assertLanguagesCount(${languageCount})")
//    }
//  }
//  code.toString()

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
