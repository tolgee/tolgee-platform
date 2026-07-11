package io.tolgee.unit.formats.properties.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.properties.out.PropertiesFileExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test
import java.io.InputStream

class PropertiesFileExporterTest {
  @Suppress("UNCHECKED_CAST")
  @Test
  fun `it exports`() {
    val exporter = getBasicExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |# I am a description
    |key = {value, plural, other {I am basic text}}
    |key.with.dots = I am key with dots
    |escaping\ test = I am key with dots = a = \n # not a comment \n = = \\ yep +áěááššá
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `honors the provided fileStructureTemplate`() {
    val exporter =
      getBasicExporter(
        params =
          getExportParams().also {
            it.fileStructureTemplate = "{languageTag}/hello/{namespace}.{extension}"
          },
      )
    val files = exporter.produceFiles()
    files["cs/hello.properties"].assert.isNotNull()
  }

  private fun getBasicExporter(params: ExportParams = getExportParams()): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key",
          text = "I am basic text",
        ) {
          key.description = "I am a description"
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "key.with.dots",
          text = "I am key with dots",
        )
        add(
          languageTag = "cs",
          keyName = "escaping test",
          text = "I am key with dots = a = \n # not a comment \n = = \\ yep +áěááššá",
        )
      }
    return getExporter(built.translations, false, params = params)
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |key3 = {count, plural, one {# den {icuParam}} few {# dny} other {# dní}}
    |item = I will be first {icuParam, number}
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}'} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first {icuParam, number}",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.properties",
      """
    |key3 = {count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}
    |item = I will be first '{'icuParam'}' {hello, number}
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): PropertiesFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {hello, number}",
        )
      }
    return getExporter(built.translations, true)
  }

  private fun Map<String, InputStream>.getFileTextContent(fileName: String): String {
    return this[fileName]!!.bufferedReader().readText()
  }

  private inline fun <reified T> Map<String, InputStream>.parseFileContent(fileName: String): T {
    return jacksonObjectMapper().readValue(this.getFileTextContent(fileName))
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    params: ExportParams = getExportParams(),
  ): PropertiesFileExporter {
    return PropertiesFileExporter(
      translations = translations,
      exportParams = params,
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
          extension = params.format?.extension ?: "properties",
        ),
    )
  }

  private fun getExportParams() = ExportParams().also { it.format = ExportFormat.PROPERTIES }
}
