package io.tolgee.unit.formats.android.out

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.tolgee.dtos.request.export.ExportParams
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.json.out.JsonFileExporterWithManifest
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.ExportFileStructureTemplateProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.unit.util.assertFile
import io.tolgee.unit.util.getExported
import io.tolgee.util.buildExportTranslationList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AndroidSdkFileExporterTest {
  @Test
  fun `exports with placeholders (ICU placeholders disabled)`() {
    val exporter = getIcuPlaceholdersDisabledExporter()
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "key3": {
    |    "one": "%1${'$'}d den %2${'$'}s",
    |    "few": "%1${'$'}d dny",
    |    "other": "%1${'$'}d dní"
    |  },
    |  "item": "I will be first %1${'$'}d"
    |}
      """.trimMargin(),
    )
  }

  private fun getIcuPlaceholdersDisabledExporter(): JsonFileExporterWithManifest {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{0, plural, one {%1${'$'}d den %2${'$'}s} few {%1${'$'}d dny} other {%1${'$'}d dní}}",
        ) {
          key.isPlural = true
        }
        add(
          languageTag = "cs",
          keyName = "item",
          text = "I will be first %1${'$'}d",
        )
      }
    return getExporter(
      built.translations,
      false,
      exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
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
    |    "one": "%1${'$'}d den %2${'$'}d",
    |    "few": "%1${'$'}d dny",
    |    "other": "%1${'$'}d dní"
    |  },
    |  "item": "I will be first {icuParam} %1${'$'}d"
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `correct exports translation with colon`() {
    val exporter =
      getExporter(
        getTranslationWithColon(),
        exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
      )
    val data = getExported(exporter)
    data.assertFile(
      "cs.json",
      """
    |{
    |  "item": "name : %1${'$'}s"
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `generates manifest json with single locale`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "key1", text = "text")
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
      )
    val data = getExported(exporter)
    data.assertFile(
      "manifest.json",
      """
    |{
    |  "locales": [ "en" ]
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `generates manifest json with multiple locales sorted`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "de", keyName = "key1", text = "text")
        add(languageTag = "en-US", keyName = "key1", text = "text")
        add(languageTag = "en", keyName = "key1", text = "text")
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
      )
    val data = getExported(exporter)
    data.assertFile(
      "manifest.json",
      """
    |{
    |  "locales": [ "de", "en", "en-US" ]
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `generates all translation JSON files alongside manifest`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "key1", text = "Hello")
        add(languageTag = "de", keyName = "key1", text = "Hallo")
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
      )
    val data = getExported(exporter)

    assertThat(data).containsKeys("en.json", "de.json", "manifest.json")

    data.assertFile(
      "en.json",
      """
    |{
    |  "key1": "Hello"
    |}
      """.trimMargin(),
    )
  }

  @Test
  fun `manifest placed at root with namespaces`() {
    val built =
      buildExportTranslationList {
        add(languageTag = "en", keyName = "common:key1", text = "text")
        add(languageTag = "de", keyName = "common:key1", text = "text")
      }
    val exporter =
      getExporter(
        built.translations,
        exportParams =
          ExportParams(
            format = ExportFormat.ANDROID_SDK,
            fileStructureTemplate = "{namespace}/{languageTag}.{extension}",
          ),
      )
    val data = getExported(exporter)

    // Manifest should be at the root level (not inside a namespace folder)
    assertThat(data).containsKey("manifest.json")

    // Verify the manifest contains both locales
    data.assertFile(
      "manifest.json",
      """
    |{
    |  "locales": [ "de", "en" ]
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

  private fun getIcuPlaceholdersEnabledExporter(): JsonFileExporterWithManifest {
    val built =
      buildExportTranslationList {
        add(
          languageTag = "cs",
          keyName = "key3",
          text = "{0, plural, one {{0, number} den {1, number}} few {{0, number} dny} other {{0, number} dní}}",
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
      exportParams = ExportParams(format = ExportFormat.ANDROID_SDK),
    )
  }

  private fun getExporter(
    translations: List<ExportTranslationView>,
    isProjectIcuPlaceholdersEnabled: Boolean = true,
    exportParams: ExportParams = ExportParams(),
  ): JsonFileExporterWithManifest {
    return JsonFileExporterWithManifest(
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
