package io.tolgee.formats.json.out

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.service.export.ExportFilePathProvider
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import java.io.InputStream

class JsonFileExporterWithManifest(
  private val translations: List<ExportTranslationView>,
  private val exportParams: IExportParams,
  private val projectIcuPlaceholdersSupport: Boolean,
  private val objectMapper: ObjectMapper,
  private val customPrettyPrinter: CustomPrettyPrinter,
  private val filePathProvider: ExportFilePathProvider,
) : FileExporter {
  private val jsonExporter =
    JsonFileExporter(
      translations = translations,
      exportParams = exportParams,
      projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
      objectMapper = objectMapper,
      customPrettyPrinter = customPrettyPrinter,
      filePathProvider = filePathProvider,
    )

  override fun produceFiles(): Map<String, InputStream> {
    val jsonFiles = jsonExporter.produceFiles()
    val manifestFile = generateManifestFile()
    return jsonFiles + ("manifest.json" to manifestFile)
  }

  private fun generateManifestFile(): InputStream {
    val locales =
      translations
        .map { it.languageTag }
        .distinct()
        .sorted()

    val manifest = mapOf("locales" to locales)

    return objectMapper
      .writer(customPrettyPrinter)
      .writeValueAsBytes(manifest)
      .inputStream()
  }
}
