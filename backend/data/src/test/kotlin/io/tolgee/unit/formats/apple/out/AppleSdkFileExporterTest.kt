package io.tolgee.unit.formats.apple.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AppleSdkFileExporterTest {
  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "key3": {
    |    "variations": {
    |      "plural": {
    |        "one": "%lld den %@",
    |        "few": "%lld dny",
    |        "other": "%lld dní"
    |      }
    |    }
    |  },
    |  "item": "I will be first %lld"
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
          text = "{0, plural, one {%lld den %@} few {%lld dny} other {%lld dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first %lld",
        )
      }
    return getExporter(
      built.translations,
      false,
      exportParams = ExportParams(format = ExportFormat.APPLE_SDK),
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
    |  "key3": {
    |    "variations": {
    |      "plural": {
    |        "one": "%lld den %2${'$'}lld",
    |        "few": "%lld dny",
    |        "other": "%lld dní"
    |      }
    |    }
    |  },
    |  "item": "I will be first {icuParam} %lld"
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `correct exports translation with colon`() {
    val exporter =
      getExporter(
        getTranslationWithColon(),
        exportParams = ExportParams(format = ExportFormat.APPLE_SDK),
      )
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "item": "name : %@"
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
          text = "name : {0}",
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
          text = "{0, plural, one {# den {1, number}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first '{'icuParam'}' {0, number}",
        )
      }
    return getExporter(
      built.translations,
      true,
      exportParams = ExportParams(format = ExportFormat.APPLE_SDK),
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
      filePathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(exportParams, translations).validateAndGetTemplate(),
          extension = exportParams.format.extension,
        ),
    )
  }
}
