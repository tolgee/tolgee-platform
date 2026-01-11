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
import io.tolgee.formats.json.out.JsonFileExporterWithManifest
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
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.CSV -> {
        CsvFileExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.JSON, ExportFormat.JSON_TOLGEE, ExportFormat.JSON_I18NEXT,
      ExportFormat.APPLE_SDK,
      -> {
        JsonFileExporter(
          data,
          exportParams,
          objectMapper = objectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter = customPrettyPrinter,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.ANDROID_SDK -> {
        JsonFileExporterWithManifest(
          translations = data,
          exportParams = exportParams,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          objectMapper = objectMapper,
          customPrettyPrinter = customPrettyPrinter,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.YAML_RUBY, ExportFormat.YAML -> {
        YamlFileExporter(
          data,
          exportParams,
          objectMapper = yamlObjectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
          customPrettyPrinter,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.XLIFF -> {
        XliffFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.APPLE_XLIFF -> {
        AppleXliffExporter(
          data,
          baseTranslationsProvider,
          baseLanguage.tag,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.ANDROID_XML -> {
        XmlResourcesExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.COMPOSE_XML -> {
        XmlResourcesExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.PO -> {
        PoFileExporter(
          data,
          exportParams,
          baseLanguage,
          projectIcuPlaceholdersSupport,
          getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.APPLE_STRINGS_STRINGSDICT -> {
        AppleStringsStringsdictExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          stringsFilePathProvider = getFilePathProvider(exportParams, data, "strings"),
          stringsdictFilePathProvider = getFilePathProvider(exportParams, data, "stringsdict"),
        )
      }

      ExportFormat.APPLE_XCSTRINGS -> {
        AppleXcstringsExporter(
          translations = data,
          exportParams = exportParams,
          objectMapper = objectMapper,
          isProjectIcuPlaceholdersEnabled = projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.FLUTTER_ARB -> {
        FlutterArbFileExporter(
          data,
          exportParams,
          baseLanguage.tag,
          objectMapper,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.PROPERTIES -> {
        PropertiesFileExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          filePathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.RESX_ICU -> {
        ResxExporter(
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          pathProvider = getFilePathProvider(exportParams, data),
        )
      }

      ExportFormat.XLSX -> {
        XlsxFileExporter(
          currentDateProvider.date,
          data,
          exportParams,
          projectIcuPlaceholdersSupport,
          pathProvider = getFilePathProvider(exportParams, data),
        )
      }
    }
  }

  fun getFilePathProvider(
    exportParams: IExportParams,
    translations: List<ExportTranslationView>,
    extension: String = exportParams.format.extension,
  ): ExportFilePathProvider {
    return ExportFilePathProvider(
      template =
        ExportFileStructureTemplateProvider(
          exportParams,
          translations,
        ).validateAndGetTemplate(),
      extension = extension,
    )
  }
}
