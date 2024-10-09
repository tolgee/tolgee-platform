package io.tolgee.service.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.android.out.AndroidStringsXmlExporter
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.formats.csv.out.CsvFileExporter
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.formats.properties.out.PropertiesFileExporter
import io.tolgee.formats.xliff.out.XliffFileExporter
import io.tolgee.formats.yaml.out.YamlFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class FileExporterFactory(
  private val objectMapper: ObjectMapper,
  @Qualifier("yamlObjectMapper")
  private val yamlObjectMapper: ObjectMapper,
  private val customPrettyPrinter: CustomPrettyPrinter,
) {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.CSV ->
        CsvFileExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.JSON, ExportFormat.JSON_TOLGEE, ExportFormat.JSON_I18NEXT ->
        JsonFileExporter(
          data,
          exportParams,
          objectMapper = objectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter = customPrettyPrinter,
        )

      ExportFormat.YAML_RUBY, ExportFormat.YAML ->
        YamlFileExporter(
          data,
          exportParams,
          objectMapper = yamlObjectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter,
        )

      ExportFormat.XLIFF ->
        XliffFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.APPLE_XLIFF ->
        AppleXliffExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage.tag,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.ANDROID_XML -> AndroidStringsXmlExporter(data, exportParams, projectIcuPlaceholdersSupport)

      ExportFormat.PO ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.APPLE_STRINGS_STRINGSDICT ->
        AppleStringsStringsdictExporter(data, exportParams, projectIcuPlaceholdersSupport)

      ExportFormat.FLUTTER_ARB ->
        FlutterArbFileExporter(
          data,
          exportParams,
          baseLanguage.tag,
          objectMapper,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.PROPERTIES ->
        PropertiesFileExporter(data, exportParams, projectIcuPlaceholdersSupport)
    }
  }
}
