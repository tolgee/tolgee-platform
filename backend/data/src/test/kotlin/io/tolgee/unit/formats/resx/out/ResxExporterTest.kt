package io.tolgee.unit.formats.resx.out

import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.resx.out.ResxExporter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.testing.assert
import io.tolgee.util.buildExportTranslationList
import org.junit.jupiter.api.Test

class ResxExporterTest {
  @Test
  fun exports() {
    val exporter = getExporter()
    val data = getExported(exporter)
    // generate this with:
    // data.map { "data.assertFile(\"${it.key}\", \"\"\"\n    |${it.value.replace("\$", "\${'$'}").replace("\n", "\n    |")}\n    \"\"\".trimMargin())" }.joinToString("\n")
    data.assertFile(
      "cs.resx",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<root>
    |  <resheader name="resmimetype">
    |    <value>text/microsoft-resx</value>
    |  </resheader>
    |  <resheader name="version">
    |    <value>2.0</value>
    |  </resheader>
    |  <resheader name="reader">
    |    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <resheader name="writer">
    |    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <data name="key1" xml:space="preserve"><value>Ahoj! I{number, number}, {name}, {number, number, scientific}, {number, number, 0.000000}</value></data>
    |  <data name="placeholders" xml:space="preserve"><value>I am {name}!</value></data>
    |  <data name="Empty plural" xml:space="preserve"/>
    |  <data name="key3" xml:space="preserve"><value>{count, plural, one {# den} few {# dny} other {# dní}}</value><comment>This is a description for plural</comment></data>
    |  <data name="forced_not plural" xml:space="preserve"><value>{count, plural, one {# den} few {# dny} other {# dní}}</value></data>
    |  <data name="i_am_array_item[20]" xml:space="preserve"><value>I will be first</value><comment>This is a description for array item</comment></data>
    |  <data name="i_am_array_item[100]" xml:space="preserve"><value>I will be second</value></data>
    |</root>
    |
      """.trimMargin(),
    )
    data.assertFile(
      "en.resx",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<root>
    |  <resheader name="resmimetype">
    |    <value>text/microsoft-resx</value>
    |  </resheader>
    |  <resheader name="version">
    |    <value>2.0</value>
    |  </resheader>
    |  <resheader name="reader">
    |    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <resheader name="writer">
    |    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <data name="i_am_array_english" xml:space="preserve"><value>This is english!</value></data>
    |  <data name="plural with placeholders" xml:space="preserve"><value>{count, plural, one {{0} dog} other {{0} dogs}}</value></data>
    |</root>
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

    files["cs/hello.resx"].assert.isNotNull()
  }

  @Test
  fun `exports with placeholders (ICU placeholders enabled)`() {
    val exporter = getIcuPlaceholdersEnabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.resx",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<root>
    |  <resheader name="resmimetype">
    |    <value>text/microsoft-resx</value>
    |  </resheader>
    |  <resheader name="version">
    |    <value>2.0</value>
    |  </resheader>
    |  <resheader name="reader">
    |    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <resheader name="writer">
    |    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <data name="key3" xml:space="preserve"><value>{count, plural, one {# den {icuParam}} few {# dny} other {# dní}}</value></data>
    |  <data name="i_am_array_item[20]" xml:space="preserve"><value>I will be first '{'icuParam'}'</value></data>
    |</root>
    |
      """.trimMargin(),
    )
  }

  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.resx",
      """
    |<?xml version="1.0" encoding="UTF-8" standalone="no"?>
    |<root>
    |  <resheader name="resmimetype">
    |    <value>text/microsoft-resx</value>
    |  </resheader>
    |  <resheader name="version">
    |    <value>2.0</value>
    |  </resheader>
    |  <resheader name="reader">
    |    <value>System.Resources.ResXResourceReader, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <resheader name="writer">
    |    <value>System.Resources.ResXResourceWriter, System.Windows.Forms, Version=4.0.0.0, Culture=neutral</value>
    |  </resheader>
    |  <data name="key3" xml:space="preserve"><value>{count, plural, one {# den {icuParam} '} few {# dny} other {# dní}}</value></data>
    |  <data name="i_am_array_item[20]" xml:space="preserve"><value>I will be first {icuParam} '{hey}'</value></data>
    |</root>
    |
      """.trimMargin(),
    )
  }

  private fun getExported(exporter: ResxExporter): Map<String, String> {
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

  private fun getExporter(params: ExportParams = getExportParams()): ResxExporter {
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
          keyName = "placeholders",
          text =
            "I am {name}!",
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
          description = "This is a description for plural",
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
          keyName = "i_am_array_item[20]",
          text = "I will be first",
          description = "This is a description for array item",
        )

        add(
          languageTag = "cs",
          keyName = "i_am_array_item[100]",
          text = "I will be second",
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

  private fun getIcuPlaceholdersEnabledExporter(): ResxExporter {
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

  private fun getIcuPlaceholdersDisabledExporter(): ResxExporter {
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
  ): ResxExporter {
    return ResxExporter(
      translations = translations,
      exportParams = params,
      isProjectIcuPlaceholdersEnabled = isProjectIcuPlaceholdersEnabled,
      pathProvider =
        ExportFilePathProvider(
          template = ExportFileStructureTemplateProvider(params, translations).validateAndGetTemplate(),
          extension = params.format?.extension ?: "resx",
        ),
    )
  }

  private fun getExportParams(): ExportParams {
    return ExportParams().also { it.format = ExportFormat.RESX_ICU }
  }
}
