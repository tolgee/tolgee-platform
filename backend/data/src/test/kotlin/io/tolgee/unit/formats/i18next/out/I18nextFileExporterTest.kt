package io.tolgee.unit.formats.i18next.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class I18nextFileExporterTest {
  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "key3_one": "# den {icuParam}",
    |  "key3_few": "# dny",
    |  "key3_other": "# dní",
    |  "item": "I will be first {icuParam, number}"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): JsonFileExporter {
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
    return getExporter(
      built.translations,
      false,
      exportParams = ExportParams(messageFormat = ExportMessageFormat.I18NEXT),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "key3_one": "{{count, number}} den {{icuParam, number}}",
    |  "key3_few": "{{count, number}} dny",
    |  "key3_other": "{{count, number}} dní",
    |  "item": "I will be first {icuParam} {{hello, number}}"
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `correct exports translation with colon`() {
    val exporter =
      getExporter(
        getTranslationWithColon(),
        exportParams = ExportParams(messageFormat = ExportMessageFormat.I18NEXT),
      )
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "item": "name : {{name}}"
    |}
      """.trimMargin(),
    )
  }

  private fun getTranslationWithColon(): MutableList<ExportTranslationView> {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "item",
          text = "name : {name}",
        )
      }
    return built.translations
  }

  private fun getIcuPlaceholdersEnabledExporter(): JsonFileExporter {
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
    return getExporter(
      built.translations,
      true,
      exportParams = ExportParams(messageFormat = ExportMessageFormat.I18NEXT),
    )
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    exportParams: ExportParams = ExportParams(),
  ): JsonFileExporter {
    return JsonFileExporter(
      translations = translations,
      exportParams = exportParams,
      projectIcuPlaceholdersSupport = isProjectIcuPlaceholdersEnabled,
      objectMapper = jacksonObjectMapper(),
      customPrettyPrinter = CustomPrettyPrinter(),
    )
  }
}
