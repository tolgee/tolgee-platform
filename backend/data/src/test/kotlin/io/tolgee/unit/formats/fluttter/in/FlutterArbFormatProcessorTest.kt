package io.tolgee.unit.formats.fluttter.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.formats.flutter.`in`.FlutterArbFileProcessor
import io.tolgee.model.dataImport.Import
import io.tolgee.model.dataImport.ImportFile
import io.tolgee.service.dataImport.processors.FileProcessorContext
import io.tolgee.testing.assert
import io.tolgee.util.assertKey
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import io.tolgee.util.custom
import io.tolgee.util.customEquals
import io.tolgee.util.description
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.io.File

class FlutterArbFormatProcessorTest {
  private lateinit var importMock: Import
  private lateinit var importFile: ImportFile
  private lateinit var importFileDto: ImportFileDto
  private lateinit var fileProcessorContext: FileProcessorContext

  @BeforeEach
  fun setup() {
    importMock = mock()
    val fileName = "app_en.arb"
    importFile = ImportFile(fileName, importMock)
    importFileDto =
      ImportFileDto(
        fileName,
        File("src/test/resources/import/flutter/app_en.arb")
          .readBytes(),
      )
    fileProcessorContext = FileProcessorContext(importFileDto, importFile)
  }

  // This is how to generate the test:
  // 1. run the test in debug mode
  // 2. copy the result of calling: generateTestsForImportResult(fileProcessorContext) from the debug window
  @Test
  fun `returns correct parsed result`() {
    FlutterArbFileProcessor(fileProcessorContext, jacksonObjectMapper()).process()
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "helloWorld")
      .assertSingle {
        hasText("Hello World!")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "dogsCount")
      .assertSinglePlural {
        hasText(
          """
          {count, plural,
          one {I have one dog.}
          other {I have {count} dogs.}
          }
          """.trimIndent(),
        )
        isPluralOptimized()
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertTranslations("en", "simpleDogCount")
      .assertSingle {
        hasText("Dogs count: {count}")
      }
    fileProcessorContext.assertLanguagesCount(1)
    fileProcessorContext.assertKey("helloWorld") {
      custom.assert.isNull()
      description.assert.isEqualTo("The conventional newborn programmer greeting")
    }
    fileProcessorContext.assertKey("dogsCount") {
      customEquals(
        """
        {
            "_flutterArbPlaceholders" : {
              "count" : {
                "type" : "int",
                "optionalParameters" : {
                  "decimalDigits" : 1
                }
              }
            }
          }
        """.trimIndent(),
      )
      description.assert.isEqualTo("The conventional newborn programmer greeting")
    }
  }
}
