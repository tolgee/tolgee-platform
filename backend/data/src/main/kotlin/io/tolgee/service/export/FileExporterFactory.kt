package io.tolgee.service.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.component.CurrentDateProvider
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.formats.apple.out.AppleXcstringsExporter
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.formats.csv.out.CsvFileExporter
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
import io.tolgee.formats.genericStructuredFile.out.CustomPrettyPrinter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.formats.po.out.PoFileExporter
import io.tolgee.formats.properties.out.PropertiesFileExporter
import io.tolgee.formats.resx.out.ResxExporter
import io.tolgee.formats.xliff.out.XliffFileExporter
import io.tolgee.formats.xlsx.out.XlsxFileExporter
import io.tolgee.formats.xmlResources.out.XmlResourcesExporter
import io.tolgee.formats.yaml.out.YamlFileExporter
import io.tolgee.service.export.dataProvider.ExportTranslationView
import io.tolgee.service.export.exporters.FileExporter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import kotlin.Int

@Component
class FileExporterFactory(
  private val objectMapper: ObjectMapper,
  @Qualifier("yamlObjectMapper")
  private val yamlObjectMapper: ObjectMapper,
  private val customPrettyPrinter: CustomPrettyPrinter,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
    projectNamespaceCount: Int,
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.CSV ->
        CsvFileExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.JSON, ExportFormat.JSON_TOLGEE, ExportFormat.JSON_I18NEXT ->
        JsonFileExporter(
          data,
          exportParams,
          objectMapper = objectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter = customPrettyPrinter,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.YAML_RUBY, ExportFormat.YAML ->
        YamlFileExporter(
          data,
          exportParams,
          objectMapper = yamlObjectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.XLIFF ->
        XliffFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.APPLE_XLIFF ->
        AppleXliffExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage.tag,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.ANDROID_XML -> XmlResourcesExporter(
        data, exportParams, projectIcuPlaceholdersSupport,
        projectNamespaceCount = projectNamespaceCount
      )

      ExportFormat.COMPOSE_XML -> XmlResourcesExporter(
        data, exportParams, projectIcuPlaceholdersSupport,
        projectNamespaceCount = projectNamespaceCount
      )

      ExportFormat.PO ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.APPLE_STRINGS_STRINGSDICT ->
        AppleStringsStringsdictExporter(
          data, exportParams, projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount
        )

      ExportFormat.APPLE_XCSTRINGS ->
        AppleXcstringsExporter(
          translations = data,
          exportParams = exportParams,
          objectMapper = objectMapper,
          isProjectIcuPlaceholdersEnabled = projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.FLUTTER_ARB ->
        FlutterArbFileExporter(
          data,
          exportParams,
          baseLanguage.tag,
          objectMapper,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )

      ExportFormat.PROPERTIES ->
        PropertiesFileExporter(
          data, exportParams, projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount
        )

      ExportFormat.RESX_ICU ->
        ResxExporter(
          data, exportParams, projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount
        )

      ExportFormat.XLSX ->
        XlsxFileExporter(
          currentDateProvider.date,
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          projectNamespaceCount = projectNamespaceCount,
        )
    }
  }
}
