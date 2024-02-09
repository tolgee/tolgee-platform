package io.tolgee.unit.formats.apple.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.apple.APPLE_FILE_ORIGINAL_CUSTOM_KEY
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AppleXliffFileExporterTest {
  @Test
  fun `exports`() {
    val exporter = getExporter()

    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()

    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data
  }

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualTo(content)
  }

  private fun getExporter(): AppleXliffExporter {
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
          keyName = "No base!",
          text = "I have no base",
        )
        add(
          languageTag = "cs",
          keyName = "No target!",
          text = null,
          baseText = "I have no target",
        )
        add(
          languageTag = "cs",
          keyName = "key2",
          text = "Namespaced",
          baseText = "Namespaced",
        ) {
          key.namespace = "homepage"
        }
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.namespace = "homepage"
        }
        add(
          languageTag = "cs",
          keyName = "key4",
          text = "Namespaced",
          baseText = "Namespaced",
        ) {
          key.namespace = "homepage"
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.strings")
        }
        add(
          languageTag = "cs",
          keyName = "key5",
          text = "{count, plural, one {# den} other {# dnů}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        )
        add(
          languageTag = "cs",
          keyName = "key <omg>",
          text = "{count, plural, one {# day} other {# days}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.xcstrings")
        }
        add(
          languageTag = "cs",
          keyName = "key6",
          text = "{count, plural, one {# den} few {dny} other {# dnů}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.custom = mapOf(APPLE_FILE_ORIGINAL_CUSTOM_KEY to "Localizable.stringsdict")
        }
      }
    return getExporter(built.translations, built.baseTranslations)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    baseTranslations: List<ExportTranslationView>,
  ): AppleXliffExporter {
    return AppleXliffExporter(
      translations = translations,
      exportParams = ExportParams(),
      baseTranslationsProvider = { baseTranslations },
      baseLanguageTag = "tag",
    )
  }
}
