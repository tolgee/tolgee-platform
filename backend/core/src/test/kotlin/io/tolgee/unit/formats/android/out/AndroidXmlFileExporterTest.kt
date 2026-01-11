package io.tolgee.unit.formats.android.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.xmlResources.XML_RESOURCES_CDATA_CUSTOM_KEY
import io.tolgee.formats.xmlResources.out.XmlResourcesExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class AndroidXmlFileExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()
    val data = getExported(exporter)
    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data.assertFile(
      "values-cs/strings.xml",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    |  <string name="key1">Ahoj! I%d, %s, %e, %f</string>
    |  <string name="percent_no_placeholders">I am just a percent \% sign!</string>
    |  <!-- This is a description -->
    |  <string name="percent_and_placeholders">I am not just a percent %s %% sign!</string>
    |  <string name="percent_and_placeholders_and_tags"><![CDATA[I am not just a percent <b>%s</b> %% sign!]]></string>
    |  <string name="forced_CDATA"><![CDATA[Forced CDATA <b>Hey!</b> sign!]]></string>
    |  <plurals name="Empty_plural">
    |    <item quantity="one"/>
    |    <item quantity="few"/>
    |    <item quantity="many"/>
    |    <item quantity="other"/>
    |  </plurals>
    |  <!-- This is a description above plural -->
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
    |    <!-- This is a description above array item -->
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
    |  <plurals name="plural_with_placeholders">
    |    <item quantity="one">%1${'$'}s dog</item>
    |    <item quantity="other">%1${'$'}s dogs</item>
    |  </plurals>
    |</resources>
    |
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

    files["cs/hello.xml"].assert.isNotNull()
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "values-cs/strings.xml",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    |  <plurals name="key3">
    |    <item quantity="one">%d den %s</item>
    |    <item quantity="few">%d dny</item>
    |    <item quantity="many">%d dní</item>
    |    <item quantity="other">%d dní</item>
    |  </plurals>
    |  <string-array name="i_am_array_item">
    |    <item>I will be first {icuParam}</item>
    |  </string-array>
    |</resources>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "values-cs/strings.xml",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
    |  <plurals name="key3">
    |    <item quantity="one"># den {icuParam} \'</item>
    |    <item quantity="few"># dny</item>
    |    <item quantity="many"># dní</item>
    |    <item quantity="other"># dní</item>
    |  </plurals>
    |  <string-array name="i_am_array_item">
    |    <item>I will be first {icuParam} \'{hey}\'</item>
    |  </string-array>
    |</resources>
    |
      """.trimMargin(),
    )
  }

  private fun getExported(exporter: XmlResourcesExporter): Map<String, String> {
    val files = exporter.produceFiles()
    val data = files.map { it.key to it.value.bufferedReader().readText() }.toMap()
    return data
  }

  private fun Map<String, String>.assertFile(
    file: String,
    content: String,
  ) {
    this[file]!!.assert.isEqualToNormalizingNewlines(content)
  }

  private fun getExporter(params: ExportParams = getExportParams()): XmlResourcesExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key1",
          text =
            "Ahoj! I" +
              "{number, number}, {name}, {number, number, scientific}, " +
              "{number, number, 0.000000}",
        )
        add(
          languageTag = "cs",
          keyName = "percent no placeholders",
          text =
            "I am just a percent % sign!",
        )
        add(
          languageTag = "cs",
          keyName = "percent and placeholders",
          text = "I am not just a percent {name} % sign!",
          description = "This is a description",
        )
        add(
          languageTag = "cs",
          keyName = "percent and placeholders and tags",
          text =
            "I am not just a percent <b>{name}</b> % sign!",
        )
        add(
          languageTag = "cs",
          keyName = "forced CDATA",
          text =
            "Forced CDATA <b>Hey!</b> sign!",
          fn = {
            key.custom = mapOf(XML_RESOURCES_CDATA_CUSTOM_KEY to true)
          },
        )
        add(
          languageTag = "cs",
          keyName = "Empty plural",
          text = null,
        ) {
          key.isPlural = true
        }

        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
          description = "This is a description above plural",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "forced_not plural",
          text = "{count, plural, one {# den} few {# dny} other {# dní}}",
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
          description = "This is a description above array item",
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
        add(
          languageTag = "en",
          keyName = "plural with placeholders",
          text = "{count, plural, one {{0} dog} other {{0} dogs}}",
        ) {
          key.isPlural = true
        }
      }
    return getExporter(built.translations, params = params)
  }

  private fun getIcuPlaceholdersEnabledExporter(): XmlResourcesExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "i_am_array_item[20]",
          text = "I will be first '{'icuParam'}'",
        )
      }
    return getExporter(built.translations, true)
  }

  private fun getIcuPlaceholdersDisabledExporter(): XmlResourcesExporter {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{count, plural, one {'#' den '{'icuParam'}' ''} few {'#' dny} other {'#' dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "i_am_array_item[20]",
          text = "I will be first {icuParam} '{hey}'",
        )
      }
    return getExporter(built.translations, false)
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    params: ExportParams = getExportParams(),
  ): XmlResourcesExporter {
    return XmlResourcesExporter(
      translations = translations,
      exportParams = params,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      ExportFilePathProvider(
        template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
        extension = params.format.extension,
      ),
    )
  }

  private fun getExportParams(): ExportParams {
    return ExportParams().also { it.format = ExportFormat.ANDROID_XML }
  }
}
