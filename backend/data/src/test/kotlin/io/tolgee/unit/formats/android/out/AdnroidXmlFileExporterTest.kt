package io.tolgee.unit.formats.android.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.android.out.AndroidStringsXmlExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AdnroidXmlFileExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()

    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")

    data.assertFile(
      "values-cs/strings.xml",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    |  <string name="key1">Ahoj! I%d, %s, %e, %f</string>
    |  <plurals name="Empty_plural">
    |    <item quantity="one"/>
    |    <item quantity="few"/>
    |    <item quantity="many"/>
    |    <item quantity="other"/>
    |  </plurals>
    |  <plurals name="key3">
    |    <item quantity="one">%d den</item>
    |    <item quantity="few">%d dny</item>
    |    <item quantity="many">%d dní</item>
    |    <item quantity="other">%d dní</item>
    |  </plurals>
    |  <string name="forced_not_plural">{count, plural, one {# den} few {# dny} other {# dní}}</string>
    |  <string name="key_with_unsupported_characters">OK!</string>
    |  <string name="unsupported_key_will_be_replaced">I have exact key name</string>
    |  <string-array name="i_am_array_item">
    |    <item>I will be first</item>
    |    <item>I will be second</item>
    |  </string-array>
    |</resources>
    |
      """.trimMargin(),
    )
    data.assertFile(
      "values-en/strings.xml",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    |  <string name="i_am_array_english">This is english!</string>
    |</resources>
    |
      """.trimMargin(),
    )
  }

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualTo(content)
  }

  private fun getExporter(): AndroidStringsXmlExporter {
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
          keyName = "Empty plural",
          text = null,
          baseText = null,
        ) {
          key.isPlural = true
        }

        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "forced_not plural",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
          baseText = "{count, plural, one {# day} other {# days}}",
        ) {
          key.isPlural = false
        }

        add(
          languageTag = "cs",
          keyName = "key!with_unsupported!characters",
          text = "OK!",
        )

        add(
          languageTag = "cs",
          // this key will be replaced by key, which has exact key name
          keyName = "unsupported_key_will_be!replaced",
          text = "I will be missing!",
        )

        add(
          languageTag = "cs",
          keyName = "unsupported_key_will_be_replaced",
          text = "I have exact key name",
        )

        add(
          languageTag = "cs",
          keyName = "unsupported_key_will_be~replaced",
          text = "I will be missing too replace it!",
        )

        add(
          languageTag = "cs",
          keyName = "i_am_array_item[20]",
          text = "I will be first",
        )

        add(
          languageTag = "cs",
          keyName = "i_am_array_item[100]",
          text = "I will be second",
        )

        add(
          languageTag = "cs",
          keyName = "i_am_array!item[106]",
          text = "I won't be added",
        )

        add(
          languageTag = "cs",
          keyName = "i_am_array~item[106]",
          text = "I won't be added",
        )
        add(
          languageTag = "en",
          keyName = "i_am_array_english",
          text = "This is english!",
        )
      }
    return getExporter(built.translations, built.baseTranslations)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    baseTranslations: List<ExportTranslationView>,
  ): AndroidStringsXmlExporter {
    return AndroidStringsXmlExporter(
      translations = translations,
      exportParams = ExportParams(),
    )
  }
}
