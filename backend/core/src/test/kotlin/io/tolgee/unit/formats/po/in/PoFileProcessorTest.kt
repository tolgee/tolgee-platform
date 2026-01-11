package io.tolgee.unit.formats.po.`in`

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.ImportFileMapping
import io.tolgee.dtos.request.SingleStepImportRequest
import io.tolgee.formats.importCommon.ImportFormat
import io.tolgee.formats.po.`in`.PoFileProcessor
import io.tolgee.unit.formats.PlaceholderConversionTestHelper
import io.tolgee.util.FileProcessorContextMockUtil
import io.tolgee.util.assertLanguagesCount
import io.tolgee.util.assertSingle
import io.tolgee.util.assertSinglePlural
import io.tolgee.util.assertTranslations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class PoFileProcessorTest {
  @BeforeEach
  fun setup() {
    mockUtil = FileProcessorContextMockUtil()
  }

  @Test
  fun `processes standard file correctly`() {
    mockImportFile("example.po")
    PoFileProcessor(mockUtil.fileProcessorContext).process()
    assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    assertThat(mockUtil.fileProcessorContext.translations).hasSize(11)
    val text =
      mockUtil.fileProcessorContext.translations["%d page read."]
        ?.get(0)
        ?.text
    assertThat(text)
      .isEqualTo(
        "{0, plural,\n" +
          "one {Eine Seite gelesen wurde.}\n" +
          "other {# Seiten gelesen wurden.}\n" +
          "}",
      )
    assertThat(
      mockUtil.fileProcessorContext.translations.values
        .toList()[2][0]
        .text,
    ).isEqualTo("Willkommen zurück, {0}! Dein letzter Besuch war am {1}")
  }

  @Test
  fun `adds metadata`() {
    mockImportFile("example.po")
    PoFileProcessor(mockUtil.fileProcessorContext).process()
    val keyMeta =
      mockUtil.fileProcessorContext.keys[
        "We connect developers and translators around the globe " +
          "in Tolgee for a fantastic localization experience.",
      ]!!.keyMeta!!
    assertThat(keyMeta.description).isEqualTo(
      "This is the text that should appear next to menu accelerators * " +
        "that use the super key. If the text on this key isn't typically * " +
        "translated on keyboards used for your language, don't translate * this.",
    )
    assertThat(keyMeta.codeReferences).hasSize(6)
    assertThat(keyMeta.codeReferences[0].path).isEqualTo("light_interface.c")
    assertThat(keyMeta.codeReferences[0].line).isEqualTo(196)
  }

  @Test
  fun `processes windows newlines`() {
    val string = jacksonObjectMapper().readValue<String>(File("src/test/resources/import/po/windows-newlines.po.json"))
    assertThat(string).contains("\r\n")

    mockImportFile("windows-newlines.po.json")
    mockUtil.fileProcessorContext.file.data = string.encodeToByteArray()
    PoFileProcessor(mockUtil.fileProcessorContext).process()
    assertThat(mockUtil.fileProcessorContext.languages).hasSize(1)
    assertThat(mockUtil.fileProcessorContext.translations).hasSize(1)
    assertThat(
      mockUtil.fileProcessorContext.translations.values
        .toList()[0][0]
        .text,
    ).isEqualTo("# Hex код (#fff)")
  }

  @Test
  fun `import with placeholder conversion (disabled ICU)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = false)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("de", "hello")
      .assertSingle {
        hasText("Hi %d {icuParam}")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("de", "%d page read.")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Hallo %d '{'icuParam'}'}
          other {Hallo %d '{'icuParam'}'}
          }
          """.trimIndent(),
        )
      }
  }

  @Test
  fun `import with placeholder conversion (no conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = false, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("de", "hello")
      .assertSingle {
        hasText("Hi %d '{'icuParam'}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("de", "%d page read.")
      .assertSinglePlural {
        hasText(
          """
          {value, plural,
          one {Hallo %d '{'icuParam'}'}
          other {Hallo %d '{'icuParam'}'}
          }
          """.trimIndent(),
        )
      }
  }

  @Test
  fun `import with placeholder conversion (with conversion)`() {
    mockPlaceholderConversionTestFile(convertPlaceholders = true, projectIcuPlaceholdersEnabled = true)
    processFile()
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext.assertLanguagesCount(1)
    mockUtil.fileProcessorContext
      .assertTranslations("de", "hello")
      .assertSingle {
        hasText("Hi {0, number} '{'icuParam'}'")
      }
    mockUtil.fileProcessorContext
      .assertTranslations("de", "%d page read.")
      .assertSinglePlural {
        hasText(
          """
          {0, plural,
          one {Hallo # '{'icuParam'}'}
          other {Hallo # '{'icuParam'}'}
          }
          """.trimIndent(),
        )
      }
  }

  @Test
  fun `placeholder conversion setting application works`() {
    PlaceholderConversionTestHelper.testFile(
      "en.po",
      "src/test/resources/import/po/example_params.po",
      assertBeforeSettingsApplication =
        listOf(
          "Hi {0, number} '{'icuParam'}'",
          "{0, plural,\none {Hallo # '{'icuParam'}'}\nother {Hallo # '{'icuParam'}'}\n}",
        ),
      assertAfterDisablingConversion =
        listOf(
          "Hi %d '{'icuParam'}'",
          "{value, plural,\none {Hallo %d '{'icuParam'}'}\nother {Hallo %d '{'icuParam'}'}\n}",
        ),
      assertAfterReEnablingConversion =
        listOf(
          "Hi {0, number} '{'icuParam'}'",
          "{0, plural,\none {Hallo # '{'icuParam'}'}\nother {Hallo # '{'icuParam'}'}\n}",
        ),
    )
  }

  @Test
  fun `respects provided format`() {
    mockUtil.mockIt("en.json", "src/test/resources/import/po/example.po")
    mockUtil.fileProcessorContext.params =
      SingleStepImportRequest().also {
        it.fileMappings = listOf(ImportFileMapping(fileName = "en.po", format = ImportFormat.PO_ICU))
      }
    processFile()
    // it's not converted to ICU
    mockUtil.fileProcessorContext.assertTranslations("de", "%d page read.")
  }

  private fun mockPlaceholderConversionTestFile(
    convertPlaceholders: Boolean,
    projectIcuPlaceholdersEnabled: Boolean,
  ) {
    mockUtil.mockIt(
      "en.po",
      "src/test/resources/import/po/example_params.po",
      convertPlaceholders,
      projectIcuPlaceholdersEnabled,
    )
  }

  private fun processFile() {
    PoFileProcessor(mockUtil.fileProcessorContext).process()
  }

  private fun mockImportFile(fileName: String) {
    mockUtil = FileProcessorContextMockUtil()
    mockUtil.mockIt("example.po", "src/test/resources/import/po/$fileName")
  }

  lateinit var mockUtil: FileProcessorContextMockUtil
}
