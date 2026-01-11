package io.tolgee.unit.formats.fluttter.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class FlutterArbFileExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()

    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
    // generate this with:
    // generateTestsForExportResult(data)

    data.assertFile(
      "app_cs.arb",
      """
    |{
    |  "@@locale" : "cs",
    |  "key1" : "Ahoj! I{number}, {name}, {number}, {number}",
    |  "key3" : "{count, plural, one {{count} den} few {{count} dny} other {{count} dní}}"
    |}
      """.trimMargin(),
    )
    data.assertFile(
      "app_en.arb",
      """
    |{
    |  "@@locale" : "en",
    |  "key3" : "{count, plural, other {{count}}}",
    |  "@key3" : {
    |    "description" : "What a count",
    |    "placeholders" : {
    |      "count" : {
    |        "type" : "int"
    |      }
    |    }
    |  }
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `honors the provided fileStructureTemplate`() {
    val exporter =
      getExporter(
        params =
          getExportParams().also {
            it.fileStructureTemplate = "{languageTag}/hello/{namespace}.{extension}"
          },
      )

    val files = exporter.produceFiles()

    files["cs/hello.arb"].assert.isNotNull()
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "app_cs.arb",
      """
    |{
    |  "@@locale" : "cs",
    |  "key3" : "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}",
    |  "item" : "I will be first {icuParam, number}"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): FlutterArbFileExporter {
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
      "app_cs.arb",
      """
    |{
    |  "@@locale" : "cs",
    |  "key3" : "{count, plural, one {{count} den {icuParam}} few {{count} dny} other {{count} dní}}",
    |  "item" : "I will be first {icuParam} {hello}"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersEnabledExporter(): FlutterArbFileExporter {
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

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualToNormalizingNewlines(content)
  }

  private fun getExporter(params: ExportParams = getExportParams()): FlutterArbFileExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key1",
          text =
            "Ahoj! I" +
              "{number, number}, {name}, {number, number, scientific}, " +
              "{number, number, 0.000000}",
          baseText =
            "Hello! I" +
              "{number, number}, {name}, {number, number, scientific}, " +
              "{number, number, 0.000000}",
        )
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
          key.custom =
            mapOf(
              "_flutterArbPlaceholders" to
                mapOf(
                  "count" to
                    mapOf(
                      "type" to "int",
                    ),
                ),
            )
          key.description = "What a count"
        }

        add(
          languageTag = "en",
          keyName = "key3",
          text = "{count}",
        ) {
          key.isPlural = true
          key.custom =
            mapOf(
              "_flutterArbPlaceholders" to
                mapOf(
                  "count" to
                    mapOf(
                      "type" to "int",
                    ),
                ),
            )
          key.description = "What a count"
        }
      }
    return getExporter(built.translations, params = params)
  }
}

private fun getExporter(translations: List<ExportTranslationView>): FlutterArbFileExporter {
  val params = getExportParams()
  return FlutterArbFileExporter(
    translations = translations,
    exportParams = params,
    baseLanguageTag = "en",
    objectMapper = jacksonObjectMapper(),
    filePathProvider =
      ExportFilePathProvider(
        template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
        extension = params.format.extension,
      ),
  )
}

private fun getExporter(
  translations: List<ExportTranslationView>,
  isProjectIcuPlaceholdersEnabled: Boolean = true,
  params: ExportParams = getExportParams(),
): FlutterArbFileExporter {
  return FlutterArbFileExporter(
    translations = translations,
    exportParams = params,
    baseLanguageTag = "en",
    objectMapper = jacksonObjectMapper(),
    isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
    filePathProvider =
      ExportFilePathProvider(
        template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
        extension = params.format.extension,
      ),
  )
}

private fun getExportParams() =
  ExportParams().also {
    it.format = ExportFormat.FLUTTER_ARB
  }

private fun getExported(exporter: FlutterArbFileExporter): Map<String, String> {
  val files = exporter.produceFiles()
  val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
  return data
}
