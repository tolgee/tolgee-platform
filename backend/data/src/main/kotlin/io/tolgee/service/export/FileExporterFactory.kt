package io.tolgee.service.export

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.constants.Message
import io.tolgee.dtos.IExportParams
import io.tolgee.dtos.cacheable.LanguageDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.formats.ExportFormat
import io.tolgee.formats.ExportMessageFormat
import io.tolgee.formats.android.out.AndroidStringsXmlExporter
import io.tolgee.formats.apple.out.AppleStringsStringsdictExporter
import io.tolgee.formats.apple.out.AppleXliffExporter
import io.tolgee.formats.flutter.out.FlutterArbFileExporter
import io.tolgee.formats.json.out.JsonFileExporter
import io.tolgee.formats.po.PoSupportedMessageFormat
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
) {
  fun create(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
  ): FileExporter {
    return when (exportParams.format) {
      ExportFormat.JSON ->
        JsonFileExporter(
          data,
          exportParams,
          objectMapper = objectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
        )

      ExportFormat.YAML_RUBY, ExportFormat.YAML_ICU, ExportFormat.YAML_JAVA ->
        YamlFileExporter(
          data,
          exportParams,
          objectMapper = yamlObjectMapper,
          projectIcuPlaceholdersSupport = projectIcuPlaceholdersSupport,
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
        getPoExporter(data, exportParams, baseTranslationsProvider, baseLanguage, projectIcuPlaceholdersSupport)

      ExportFormat.PO_PHP ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          PoSupportedMessageFormat.PHP,
          projectIcuPlaceholdersSupport,
        )

      ExportFormat.PO_C ->
        PoFileExporter(
          data,
          exportParams,
          baseTranslationsProvider,
          baseLanguage,
          PoSupportedMessageFormat.C,
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

  private fun getPoExporter(
    data: List<ExportTranslationView>,
    exportParams: IExportParams,
    baseTranslationsProvider: () -> List<ExportTranslationView>,
    baseLanguage: LanguageDto,
    projectIcuPlaceholdersSupport: Boolean,
  ): PoFileExporter {
    val poSupportedMessageFormat =
      when (exportParams.messageFormat) {
        null -> PoSupportedMessageFormat.C
        ExportMessageFormat.PHP_SPRINTF -> PoSupportedMessageFormat.PHP
        ExportMessageFormat.C_SPRINTF -> PoSupportedMessageFormat.C
//        ExportMessageFormat.PYTHON_SPRINTF -> PoSupportedMessageFormat.PYTHON
        else -> throw BadRequestException(Message.UNSUPPORTED_PO_MESSAGE_FORMAT)
      }
    return PoFileExporter(
      data,
      exportParams,
      baseTranslationsProvider,
      baseLanguage,
      poSupportedMessageFormat,
      projectIcuPlaceholdersSupport,
    )
  }
}
