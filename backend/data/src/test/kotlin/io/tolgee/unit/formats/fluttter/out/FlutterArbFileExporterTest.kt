package io.tolgee.unit.formats.fluttter.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
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
    |  "key3" : "{0, plural, one {{count} den} few {{count} dny} other {{count} dní}}"
    |}
      """.trimMargin(),
    )
    data.assertFile(
      "app_en.arb",
      """
    |{
    |  "@@locale" : "en",
    |  "key3" : "{0, plural, other {{count}}}",
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

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualTo(content)
  }

  private fun getExporter(): FlutterArbFileExporter {
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
    return getExporter(built.translations)
  }
}

private fun getExporter(translations: List<ExportTranslationView>): FlutterArbFileExporter {
  return FlutterArbFileExporter(
    translations = translations,
    exportParams = ExportParams(),
    baseLanguageTag = "en",
    objectMapper = jacksonObjectMapper(),
  )
}
