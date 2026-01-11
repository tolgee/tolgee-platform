package io.tolgee.unit.formats.csv.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.csv.out.CsvFileExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class CsvFileExporterTest {
  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "all.csv",
      """
    |"key","cs"
    |"key3","{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}"
    |"item","I will be first {icuParam, number}"
    |"key","Text with multiple lines
    |and , commas and ""quotes"" "
    |
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): CsvFileExporter {
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
        add(
          languageTag = "cs",
          keyName = "key",
          text = "Text with multiple lines\nand , commas and \"quotes\" ",
        )
      }
    return getExporter(built.translations, false)
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "all.csv",
      """
    |"key","cs"
    |"key3","{count, plural, one {# den {icuParam, number}} few {# dny} other {# dní}}"
    |"item","I will be first '{'icuParam'}' {hello, number}"
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `correct exports translation with colon`() {
    val exporter = getExporter(getTranslationWithColon())
    val data = getExported(exporter)
    data.assertFile(
      "all.csv",
      """
    |"key","cs"
    |"item","name : {name}"
    |
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

  private fun getIcuPlaceholdersEnabledExporter(): CsvFileExporter {
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

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
  ): CsvFileExporter {
    val exportParams =
      ExportParams().apply {
        format = ExportFormat.CSV
      }

    return CsvFileExporter(
      translations = translations,
      exportParams = exportParams,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(exportParams, translations).validateAndGetTemplate(),
          extension = exportParams.format.extension,
        ),
    )
  }
}
